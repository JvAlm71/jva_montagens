package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.*;
import com.java10x.jvaMontagens.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FinancialService {
    private final FinancialRepository financialRepository;
    private final ParkRepository parkRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ClientRepository clientRepository;
    private final ServiceEntryRepository serviceEntryRepository;
    private final PaymentEntryRepository paymentEntryRepository;

    public FinancialService(
            FinancialRepository financialRepository,
            ParkRepository parkRepository,
            FuncionarioRepository funcionarioRepository,
            ClientRepository clientRepository,
            ServiceEntryRepository serviceEntryRepository,
            PaymentEntryRepository paymentEntryRepository
    ) {
        this.financialRepository = financialRepository;
        this.parkRepository = parkRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.clientRepository = clientRepository;
        this.serviceEntryRepository = serviceEntryRepository;
        this.paymentEntryRepository = paymentEntryRepository;
    }

    public FinancialModel createFinancialPeriod(CreateFinancialPeriodInput input) {
        validateMonth(input.month());
        validateYear(input.year());

        if (financialRepository.existsByParkIdAndYearAndMonth(input.parkId(), input.year(), input.month())) {
            throw new IllegalArgumentException("A financial period already exists for this park/month/year.");
        }

        ParkModel park = parkRepository.findById(input.parkId())
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + input.parkId()));

        FuncionariosModel administrator = null;
        if (input.administratorId() != null) {
            administrator = funcionarioRepository.findById(input.administratorId())
                    .orElseThrow(() -> new NoSuchElementException("Administrator not found for id " + input.administratorId()));
        }

        FinancialModel financial = new FinancialModel();
        financial.setPark(park);
        financial.setAdministrator(administrator);
        financial.setYear(input.year());
        financial.setMonth(input.month());
        BigDecimal jvaPricePerMeter = zeroIfNull(input.jvaPricePerMeter());
        BigDecimal leaderPricePerMeter = zeroIfNull(input.leaderPricePerMeter());
        BigDecimal taxRate = normalizeRate(zeroIfNull(input.taxRate()));
        BigDecimal carRentalValue = zeroIfNull(input.carRentalValue());

        validateNonNegative(jvaPricePerMeter, "jvaPricePerMeter");
        validateNonNegative(leaderPricePerMeter, "leaderPricePerMeter");
        validateNonNegative(taxRate, "taxRate");
        validateNonNegative(carRentalValue, "carRentalValue");

        financial.setJvaPricePerMeter(jvaPricePerMeter);
        financial.setLeaderPricePerMeter(leaderPricePerMeter);
        financial.setTaxRate(taxRate);
        financial.setCarRentalValue(carRentalValue);
        financial.setStatus(input.status() == null ? FinancialStatus.OPEN : input.status());

        return financialRepository.save(financial);
    }

    public List<FinancialModel> listPeriods(Long parkId) {
        if (parkId != null) {
            return financialRepository.findByParkIdOrderByYearDescMonthDesc(parkId);
        }
        return financialRepository.findAll();
    }

    public FinancialModel getPeriod(Long financialId) {
        return financialRepository.findById(financialId)
                .orElseThrow(() -> new NoSuchElementException("Financial period not found for id " + financialId));
    }

    public List<ServiceEntryModel> listServiceEntries(Long financialId) {
        getPeriod(financialId);
        return serviceEntryRepository.findByFinancialId(financialId);
    }

    public List<PaymentEntryModel> listPaymentEntries(Long financialId) {
        getPeriod(financialId);
        return paymentEntryRepository.findByFinancialId(financialId);
    }

    public ServiceEntryModel addServiceEntry(Long financialId, CreateServiceEntryInput input) {
        FinancialModel financial = getPeriod(financialId);

        FuncionariosModel leader = null;
        if (input.leaderId() != null) {
            leader = funcionarioRepository.findById(input.leaderId())
                    .orElseThrow(() -> new NoSuchElementException("Leader not found for id " + input.leaderId()));
        }

        BigDecimal meters = zeroIfNull(input.meters());
        BigDecimal unitPrice = input.unitPrice() == null ? financial.getJvaPricePerMeter() : input.unitPrice();
        BigDecimal grossValue = input.grossValue() == null
                ? meters.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
                : input.grossValue();

        validatePositive(meters, "meters");
        validateNonNegative(unitPrice, "unitPrice");
        validateNonNegative(grossValue, "grossValue");

        Integer days = input.days() != null ? input.days() : calculateDays(input.startDate(), input.endDate());
        validateDays(days);

        ServiceEntryModel serviceEntry = new ServiceEntryModel();
        serviceEntry.setFinancial(financial);
        serviceEntry.setServiceType(input.serviceType() == null ? ServiceType.ASSEMBLY : input.serviceType());
        serviceEntry.setTeamType(input.teamType() == null || input.teamType().isBlank() ? "UNSPECIFIED" : input.teamType());
        serviceEntry.setLeader(leader);
        serviceEntry.setMeters(meters);
        serviceEntry.setUnitPrice(unitPrice);
        serviceEntry.setGrossValue(grossValue);
        serviceEntry.setNotes(input.notes());
        serviceEntry.setStartDate(input.startDate());
        serviceEntry.setEndDate(input.endDate());
        serviceEntry.setDays(days);

        if (input.helpers() != null) {
            for (ServiceHelperInput helperInput : input.helpers()) {
                FuncionariosModel helperEmployee = funcionarioRepository.findById(helperInput.employeeId())
                        .orElseThrow(() -> new NoSuchElementException("Helper employee not found for id " + helperInput.employeeId()));

                BigDecimal dailyRateUsed = helperInput.dailyRateUsed() != null
                        ? helperInput.dailyRateUsed()
                        : zeroIfNull(helperEmployee.getDailyRate());

                Integer daysUsed = helperInput.daysUsed() != null
                        ? helperInput.daysUsed()
                        : (days == null ? 0 : days);

                BigDecimal totalCost = helperInput.totalCost() != null
                        ? helperInput.totalCost()
                        : dailyRateUsed.multiply(BigDecimal.valueOf(daysUsed)).setScale(2, RoundingMode.HALF_UP);

                validateNonNegative(dailyRateUsed, "dailyRateUsed");
                validateNonNegative(totalCost, "totalCost");
                if (daysUsed < 0) {
                    throw new IllegalArgumentException("daysUsed cannot be negative.");
                }

                ServiceHelperModel serviceHelper = new ServiceHelperModel();
                serviceHelper.setServiceEntry(serviceEntry);
                serviceHelper.setEmployee(helperEmployee);
                serviceHelper.setDailyRateUsed(dailyRateUsed);
                serviceHelper.setDaysUsed(daysUsed);
                serviceHelper.setTotalCost(totalCost);

                serviceEntry.getHelpers().add(serviceHelper);
            }
        }

        return serviceEntryRepository.save(serviceEntry);
    }

    public PaymentEntryModel addPaymentEntry(Long financialId, CreatePaymentEntryInput input) {
        FinancialModel financial = getPeriod(financialId);

        FuncionariosModel employee = null;
        if (input.employeeId() != null) {
            employee = funcionarioRepository.findById(input.employeeId())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found for id " + input.employeeId()));
        }

        ClientModel client = null;
        if (input.clientCnpj() != null) {
            String normalizedCnpj = DocumentUtils.normalizeCnpj(input.clientCnpj());
            client = clientRepository.findById(normalizedCnpj)
                    .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));
        }

        if (input.name() == null || input.name().isBlank()) {
            throw new IllegalArgumentException("Payment name is required.");
        }

        BigDecimal amount = zeroIfNull(input.amount());
        validatePositive(amount, "amount");

        PaymentEntryModel payment = new PaymentEntryModel();
        payment.setFinancial(financial);
        payment.setPaymentDate(input.paymentDate() == null ? LocalDate.now() : input.paymentDate());
        payment.setName(input.name().trim());
        payment.setInvoiceNumber(input.invoiceNumber());
        payment.setAmount(amount);
        payment.setCategory(input.category() == null ? PaymentCategory.OTHER : input.category());
        payment.setNotes(input.notes());
        payment.setEmployee(employee);
        payment.setClient(client);

        return paymentEntryRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public FinancialSummary calculateSummary(Long financialId) {
        FinancialModel financial = getPeriod(financialId);
        return calculateSummaryForPeriod(financial);
    }

    @Transactional(readOnly = true)
    public ParkFinancialOverview calculateParkOverview(Long parkId) {
        ParkModel park = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));

        List<FinancialModel> periods = financialRepository.findByParkIdOrderByYearDescMonthDesc(parkId);
        List<ParkPeriodSummary> periodsSummary = periods.stream()
                .map(period -> {
                    FinancialSummary summary = calculateSummaryForPeriod(period);
                    return new ParkPeriodSummary(
                            period.getId(),
                            period.getYear(),
                            period.getMonth(),
                            period.getStatus(),
                            summary.grossRevenue().add(summary.carRentalValue()),
                            summary.totalCost(),
                            summary.netRevenue(),
                            summary.marginPercent(),
                            summary.totalServices(),
                            summary.totalPayments()
                    );
                })
                .toList();

        BigDecimal totalInflow = periodsSummary.stream()
                .map(ParkPeriodSummary::inflow)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutflow = periodsSummary.stream()
                .map(ParkPeriodSummary::outflow)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBalance = totalInflow.subtract(totalOutflow).setScale(2, RoundingMode.HALF_UP);

        return new ParkFinancialOverview(
                park.getId(),
                park.getName(),
                periodsSummary.size(),
                totalInflow.setScale(2, RoundingMode.HALF_UP),
                totalOutflow.setScale(2, RoundingMode.HALF_UP),
                totalBalance,
                periodsSummary
        );
    }

    private FinancialSummary calculateSummaryForPeriod(FinancialModel financial) {
        Long financialId = financial.getId();
        List<ServiceEntryModel> services = serviceEntryRepository.findByFinancialId(financialId);
        List<PaymentEntryModel> payments = paymentEntryRepository.findByFinancialId(financialId);

        BigDecimal totalMeters = services.stream()
                .map(ServiceEntryModel::getMeters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossRevenue = services.stream()
                .map(ServiceEntryModel::getGrossValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal helpersCost = services.stream()
                .flatMap(service -> service.getHelpers().stream())
                .map(ServiceHelperModel::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal leaderCost = totalMeters
                .multiply(zeroIfNull(financial.getLeaderPricePerMeter()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxes = grossRevenue
                .multiply(zeroIfNull(financial.getTaxRate()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal additionalPayments = payments.stream()
                .map(PaymentEntryModel::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal carRentalIncome = zeroIfNull(financial.getCarRentalValue());

        BigDecimal totalCost = helpersCost
                .add(leaderCost)
                .add(taxes)
                .add(additionalPayments);

        BigDecimal netRevenue = grossRevenue
                .add(carRentalIncome)
                .subtract(totalCost)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalIncome = grossRevenue.add(carRentalIncome);
        BigDecimal marginPercent = totalIncome.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : netRevenue.multiply(BigDecimal.valueOf(100))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);

        return new FinancialSummary(
                financialId,
                services.size(),
                payments.size(),
                totalMeters.setScale(2, RoundingMode.HALF_UP),
                grossRevenue.setScale(2, RoundingMode.HALF_UP),
                helpersCost.setScale(2, RoundingMode.HALF_UP),
                leaderCost,
                taxes,
                carRentalIncome.setScale(2, RoundingMode.HALF_UP),
                additionalPayments.setScale(2, RoundingMode.HALF_UP),
                totalCost.setScale(2, RoundingMode.HALF_UP),
                netRevenue,
                marginPercent
        );
    }

    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
    }

    private void validateYear(Integer year) {
        if (year == null || year < 2020 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2020 and 2100.");
        }
    }

    private void validateDays(Integer days) {
        if (days != null && days < 0) {
            throw new IllegalArgumentException("days cannot be negative.");
        }
    }

    private void validatePositive(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero.");
        }
    }

    private void validateNonNegative(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " cannot be negative.");
        }
    }

    private Integer calculateDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1L);
    }

    private BigDecimal normalizeRate(BigDecimal taxRate) {
        if (taxRate.compareTo(BigDecimal.ONE) > 0) {
            return taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        return taxRate.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Transactional
    public FinancialModel updatePeriod(Long periodId, UpdateFinancialPeriodInput input) {
        FinancialModel financial = getPeriod(periodId);

        if (input.jvaPricePerMeter() != null) {
            validateNonNegative(input.jvaPricePerMeter(), "jvaPricePerMeter");
            financial.setJvaPricePerMeter(input.jvaPricePerMeter());
        }
        if (input.leaderPricePerMeter() != null) {
            validateNonNegative(input.leaderPricePerMeter(), "leaderPricePerMeter");
            financial.setLeaderPricePerMeter(input.leaderPricePerMeter());
        }
        if (input.taxRate() != null) {
            BigDecimal taxRate = normalizeRate(input.taxRate());
            validateNonNegative(taxRate, "taxRate");
            financial.setTaxRate(taxRate);
        }
        if (input.carRentalValue() != null) {
            validateNonNegative(input.carRentalValue(), "carRentalValue");
            financial.setCarRentalValue(input.carRentalValue());
        }
        if (input.status() != null) {
            financial.setStatus(input.status());
        }
        return financialRepository.save(financial);
    }

    @Transactional
    public void deletePeriod(Long periodId) {
        FinancialModel financial = getPeriod(periodId);
        financialRepository.delete(financial);
    }

    @Transactional
    public ServiceEntryModel updateServiceEntry(Long serviceId, UpdateServiceEntryInput input) {
        ServiceEntryModel service = serviceEntryRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service entry not found for id " + serviceId));

        if (input.serviceType() != null) service.setServiceType(input.serviceType());
        if (input.teamType() != null) service.setTeamType(input.teamType());
        if (input.meters() != null) {
            validatePositive(input.meters(), "meters");
            service.setMeters(input.meters());
        }
        if (input.unitPrice() != null) {
            validateNonNegative(input.unitPrice(), "unitPrice");
            service.setUnitPrice(input.unitPrice());
        }
        if (input.grossValue() != null) {
            validateNonNegative(input.grossValue(), "grossValue");
            service.setGrossValue(input.grossValue());
        } else if (input.meters() != null || input.unitPrice() != null) {
            BigDecimal m = input.meters() != null ? input.meters() : service.getMeters();
            BigDecimal u = input.unitPrice() != null ? input.unitPrice() : service.getUnitPrice();
            service.setGrossValue(m.multiply(u).setScale(2, RoundingMode.HALF_UP));
        }
        if (input.notes() != null) service.setNotes(input.notes());
        if (input.startDate() != null) service.setStartDate(input.startDate());
        if (input.endDate() != null) service.setEndDate(input.endDate());
        if (input.days() != null) service.setDays(input.days());

        return serviceEntryRepository.save(service);
    }

    @Transactional
    public void deleteServiceEntry(Long serviceId) {
        ServiceEntryModel service = serviceEntryRepository.findById(serviceId)
                .orElseThrow(() -> new NoSuchElementException("Service entry not found for id " + serviceId));
        serviceEntryRepository.delete(service);
    }

    @Transactional
    public PaymentEntryModel updatePaymentEntry(Long paymentId, UpdatePaymentEntryInput input) {
        PaymentEntryModel payment = paymentEntryRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment entry not found for id " + paymentId));

        if (input.paymentDate() != null) payment.setPaymentDate(input.paymentDate());
        if (input.name() != null && !input.name().isBlank()) payment.setName(input.name().trim());
        if (input.invoiceNumber() != null) payment.setInvoiceNumber(input.invoiceNumber());
        if (input.amount() != null) {
            validatePositive(input.amount(), "amount");
            payment.setAmount(input.amount());
        }
        if (input.category() != null) payment.setCategory(input.category());
        if (input.notes() != null) payment.setNotes(input.notes());

        return paymentEntryRepository.save(payment);
    }

    @Transactional
    public void deletePaymentEntry(Long paymentId) {
        PaymentEntryModel payment = paymentEntryRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment entry not found for id " + paymentId));
        paymentEntryRepository.delete(payment);
    }

    public record CreateFinancialPeriodInput(
            Long parkId,
            Integer year,
            Integer month,
            BigDecimal jvaPricePerMeter,
            BigDecimal leaderPricePerMeter,
            BigDecimal taxRate,
            BigDecimal carRentalValue,
            FinancialStatus status,
            Long administratorId
    ) {}

    public record ServiceHelperInput(
            Long employeeId,
            BigDecimal dailyRateUsed,
            Integer daysUsed,
            BigDecimal totalCost
    ) {}

    public record CreateServiceEntryInput(
            ServiceType serviceType,
            String teamType,
            Long leaderId,
            BigDecimal meters,
            BigDecimal unitPrice,
            BigDecimal grossValue,
            String notes,
            LocalDate startDate,
            LocalDate endDate,
            Integer days,
            List<ServiceHelperInput> helpers
    ) {}

    public record CreatePaymentEntryInput(
            LocalDate paymentDate,
            String name,
            String invoiceNumber,
            BigDecimal amount,
            PaymentCategory category,
            String notes,
            Long employeeId,
            String clientCnpj
    ) {}

    public record FinancialSummary(
            Long financialId,
            Integer totalServices,
            Integer totalPayments,
            BigDecimal totalMeters,
            BigDecimal grossRevenue,
            BigDecimal helpersCost,
            BigDecimal leaderCost,
            BigDecimal taxValue,
            BigDecimal carRentalValue,
            BigDecimal additionalPayments,
            BigDecimal totalCost,
            BigDecimal netRevenue,
            BigDecimal marginPercent
    ) {}

    public record ParkPeriodSummary(
            Long periodId,
            Integer year,
            Integer month,
            FinancialStatus status,
            BigDecimal inflow,
            BigDecimal outflow,
            BigDecimal balance,
            BigDecimal marginPercent,
            Integer totalServices,
            Integer totalPayments
    ) {}

    public record ParkFinancialOverview(
            Long parkId,
            String parkName,
            Integer totalPeriods,
            BigDecimal totalInflow,
            BigDecimal totalOutflow,
            BigDecimal totalBalance,
            List<ParkPeriodSummary> periods
    ) {}

    public record UpdateFinancialPeriodInput(
            BigDecimal jvaPricePerMeter,
            BigDecimal leaderPricePerMeter,
            BigDecimal taxRate,
            BigDecimal carRentalValue,
            FinancialStatus status
    ) {}

    public record UpdateServiceEntryInput(
            ServiceType serviceType,
            String teamType,
            BigDecimal meters,
            BigDecimal unitPrice,
            BigDecimal grossValue,
            String notes,
            LocalDate startDate,
            LocalDate endDate,
            Integer days
    ) {}

    public record UpdatePaymentEntryInput(
            LocalDate paymentDate,
            String name,
            String invoiceNumber,
            BigDecimal amount,
            PaymentCategory category,
            String notes
    ) {}
}
