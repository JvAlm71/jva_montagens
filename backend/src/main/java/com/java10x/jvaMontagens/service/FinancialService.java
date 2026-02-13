package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.*;
import com.java10x.jvaMontagens.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

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
        FuncionariosModel leader = resolveLeaderForService(financial, input.leaderId());

        BigDecimal meters = zeroIfNull(input.meters());
        BigDecimal unitPrice = zeroIfNull(financial.getJvaPricePerMeter());
        BigDecimal grossValue = meters.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

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
                if (helperInput == null || helperInput.employeeId() == null) {
                    throw new IllegalArgumentException("Helper employeeId is required.");
                }

                FuncionariosModel helperEmployee = funcionarioRepository.findById(helperInput.employeeId())
                        .orElseThrow(() -> new NoSuchElementException("Helper employee not found for id " + helperInput.employeeId()));
                validateEmployeeRole(helperEmployee, JobRole.ASSEMBLER, "Helper employee");

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
        PaymentCategory category = input.category() == null ? PaymentCategory.OTHER : input.category();
        FuncionariosModel employee = resolvePaymentEmployee(category, input.employeeId());

        ClientModel client = resolveClient(input.clientCnpj());
        if (category == PaymentCategory.CLIENT_PAYMENT && client == null) {
            client = financial.getPark().getClient();
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
        payment.setCategory(category);
        payment.setNotes(input.notes());
        payment.setEmployee(employee);
        payment.setClient(client);
        payment.setHasReceipt(false);

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

    @Transactional(readOnly = true)
    public CarRentalSummary summarizeCarRental(Long parkId) {
        ParkModel park = null;
        List<FinancialModel> periods;
        if (parkId != null) {
            park = parkRepository.findById(parkId)
                    .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));
            periods = financialRepository.findByParkIdOrderByYearDescMonthDesc(parkId);
        } else {
            periods = financialRepository.findAll();
        }

        Map<YearMonth, BigDecimal> monthlyTotals = new TreeMap<>(Comparator.reverseOrder());
        Map<Integer, BigDecimal> annualTotals = new TreeMap<>(Comparator.reverseOrder());

        List<CarRentalPeriodTotal> periodTotals = periods.stream()
                .map(period -> {
                    BigDecimal value = zeroIfNull(period.getCarRentalValue());
                    YearMonth key = YearMonth.of(period.getYear(), period.getMonth());
                    monthlyTotals.merge(key, value, BigDecimal::add);
                    annualTotals.merge(period.getYear(), value, BigDecimal::add);

                    return new CarRentalPeriodTotal(
                            period.getId(),
                            period.getPark().getId(),
                            period.getPark().getName(),
                            period.getYear(),
                            period.getMonth(),
                            value.setScale(2, RoundingMode.HALF_UP)
                    );
                })
                .sorted(Comparator
                        .comparing(CarRentalPeriodTotal::year).reversed()
                        .thenComparing(CarRentalPeriodTotal::month, Comparator.reverseOrder())
                        .thenComparing(CarRentalPeriodTotal::periodId, Comparator.reverseOrder()))
                .toList();

        List<CarRentalMonthTotal> monthlyBreakdown = monthlyTotals.entrySet().stream()
                .map(entry -> new CarRentalMonthTotal(
                        entry.getKey().getYear(),
                        entry.getKey().getMonthValue(),
                        entry.getValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        List<CarRentalYearTotal> annualBreakdown = annualTotals.entrySet().stream()
                .map(entry -> new CarRentalYearTotal(
                        entry.getKey(),
                        entry.getValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        BigDecimal totalAllTime = annualBreakdown.stream()
                .map(CarRentalYearTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int currentYear = LocalDate.now().getYear();
        BigDecimal currentYearTotal = annualTotals.getOrDefault(currentYear, BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new CarRentalSummary(
                park == null ? null : park.getId(),
                park == null ? null : park.getName(),
                totalAllTime,
                currentYearTotal,
                annualBreakdown,
                monthlyBreakdown,
                periodTotals
        );
    }

    private FinancialSummary calculateSummaryForPeriod(FinancialModel financial) {
        Long financialId = financial.getId();
        List<ServiceEntryModel> services = serviceEntryRepository.findByFinancialId(financialId);
        List<PaymentEntryModel> payments = paymentEntryRepository.findByFinancialId(financialId);

        BigDecimal totalMeters = services.stream()
                .map(ServiceEntryModel::getMeters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossRevenue = totalMeters
                .multiply(zeroIfNull(financial.getJvaPricePerMeter()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal helpersCost = services.stream()
                .flatMap(service -> service.getHelpers().stream())
                .map(ServiceHelperModel::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, LeaderAccumulator> leaderAccumulatorMap = new TreeMap<>();
        BigDecimal leaderCost = BigDecimal.ZERO;
        for (ServiceEntryModel service : services) {
            FuncionariosModel leader = service.getLeader();
            if (leader == null) continue;

            BigDecimal serviceMeters = zeroIfNull(service.getMeters());
            BigDecimal leaderRate = resolveLeaderRate(leader, financial);
            BigDecimal serviceLeaderEarning = serviceMeters
                    .multiply(leaderRate)
                    .setScale(2, RoundingMode.HALF_UP);

            leaderCost = leaderCost.add(serviceLeaderEarning);

            LeaderAccumulator current = leaderAccumulatorMap.get(leader.getId());
            if (current == null) {
                current = new LeaderAccumulator(
                        leader.getId(),
                        leader.getName(),
                        BigDecimal.ZERO,
                        leaderRate,
                        BigDecimal.ZERO
                );
            }
            leaderAccumulatorMap.put(
                    leader.getId(),
                    new LeaderAccumulator(
                            current.leaderId(),
                            current.leaderName(),
                            current.totalMeters().add(serviceMeters),
                            current.rateUsed(),
                            current.totalEarnings().add(serviceLeaderEarning)
                    )
            );
        }

        List<LeaderEarningSummary> leaderEarnings = leaderAccumulatorMap.values().stream()
                .map(item -> new LeaderEarningSummary(
                        item.leaderId(),
                        item.leaderName(),
                        item.totalMeters().setScale(2, RoundingMode.HALF_UP),
                        item.rateUsed().setScale(2, RoundingMode.HALF_UP),
                        item.totalEarnings().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        BigDecimal taxes = grossRevenue
                .multiply(zeroIfNull(financial.getTaxRate()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal clientPaymentsReceived = payments.stream()
                .filter(payment -> payment.getCategory() == PaymentCategory.CLIENT_PAYMENT)
                .map(PaymentEntryModel::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal additionalPayments = payments.stream()
                .filter(payment -> payment.getCategory() != PaymentCategory.CLIENT_PAYMENT)
                .map(PaymentEntryModel::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal carRentalIncome = zeroIfNull(financial.getCarRentalValue());
        BigDecimal expectedClientBilling = grossRevenue.add(carRentalIncome).setScale(2, RoundingMode.HALF_UP);
        BigDecimal clientBalancePending = expectedClientBilling
                .subtract(clientPaymentsReceived)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCost = helpersCost
                .add(leaderCost)
                .add(taxes)
                .add(additionalPayments);

        BigDecimal netRevenue = grossRevenue
                .add(carRentalIncome)
                .subtract(totalCost)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalIncome = expectedClientBilling;
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
                leaderCost.setScale(2, RoundingMode.HALF_UP),
                leaderEarnings,
                taxes,
                carRentalIncome.setScale(2, RoundingMode.HALF_UP),
                clientPaymentsReceived.setScale(2, RoundingMode.HALF_UP),
                clientBalancePending,
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

    private FuncionariosModel resolveLeaderForService(FinancialModel financial, Long leaderId) {
        boolean requiresLeader = zeroIfNull(financial.getLeaderPricePerMeter()).compareTo(BigDecimal.ZERO) > 0;
        if (leaderId == null || leaderId <= 0) {
            if (requiresLeader) {
                throw new IllegalArgumentException("leaderId is required when leaderPricePerMeter is greater than zero.");
            }
            return null;
        }

        FuncionariosModel leader = funcionarioRepository.findById(leaderId)
                .orElseThrow(() -> new NoSuchElementException("Leader not found for id " + leaderId));
        validateEmployeeRole(leader, JobRole.LEADER, "Leader");
        return leader;
    }

    private void validateEmployeeRole(FuncionariosModel employee, JobRole expectedRole, String context) {
        if (employee.getRole() != expectedRole) {
            throw new IllegalArgumentException(context + " must have role " + expectedRole.name() + ".");
        }
        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new IllegalArgumentException(context + " must be active.");
        }
    }

    private FuncionariosModel resolvePaymentEmployee(PaymentCategory category, Long employeeId) {
        if (category != PaymentCategory.EMPLOYEE_HELPER && category != PaymentCategory.EMPLOYEE_LEADER) {
            return null;
        }

        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("employeeId is required for " + category.name() + " category.");
        }

        FuncionariosModel employee = funcionarioRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found for id " + employeeId));

        JobRole expectedRole = category == PaymentCategory.EMPLOYEE_HELPER ? JobRole.ASSEMBLER : JobRole.LEADER;
        validateEmployeeRole(employee, expectedRole, "Payment employee");
        return employee;
    }

    private BigDecimal resolveLeaderRate(FuncionariosModel leader, FinancialModel financial) {
        BigDecimal employeeRate = leader.getPricePerMeter();
        if (employeeRate != null && employeeRate.compareTo(BigDecimal.ZERO) > 0) {
            return employeeRate;
        }
        return zeroIfNull(financial.getLeaderPricePerMeter());
    }

    private ClientModel resolveClient(String clientCnpj) {
        if (clientCnpj == null) return null;
        String trimmedCnpj = clientCnpj.trim();
        if (trimmedCnpj.isEmpty()) return null;

        String normalizedCnpj = DocumentUtils.normalizeCnpj(trimmedCnpj);
        return clientRepository.findById(normalizedCnpj)
                .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));
    }

    private record LeaderAccumulator(
            Long leaderId,
            String leaderName,
            BigDecimal totalMeters,
            BigDecimal rateUsed,
            BigDecimal totalEarnings
    ) {}

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
        BigDecimal newLeaderPricePerMeter = input.leaderPricePerMeter() != null
                ? input.leaderPricePerMeter()
                : financial.getLeaderPricePerMeter();
        boolean jvaPriceUpdated = false;

        if (input.jvaPricePerMeter() != null) {
            validateNonNegative(input.jvaPricePerMeter(), "jvaPricePerMeter");
            financial.setJvaPricePerMeter(input.jvaPricePerMeter());
            jvaPriceUpdated = true;
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

        if (zeroIfNull(newLeaderPricePerMeter).compareTo(BigDecimal.ZERO) > 0
                && serviceEntryRepository.existsByFinancialIdAndLeaderIsNull(periodId)) {
            throw new IllegalArgumentException("All services must have leaderId when leaderPricePerMeter is greater than zero.");
        }

        if (jvaPriceUpdated) {
            BigDecimal currentJvaPrice = zeroIfNull(financial.getJvaPricePerMeter());
            List<ServiceEntryModel> services = serviceEntryRepository.findByFinancialId(periodId);
            for (ServiceEntryModel service : services) {
                service.setUnitPrice(currentJvaPrice);
                service.setGrossValue(zeroIfNull(service.getMeters()).multiply(currentJvaPrice).setScale(2, RoundingMode.HALF_UP));
            }
            serviceEntryRepository.saveAll(services);
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
        if (input.leaderId() != null) {
            service.setLeader(resolveLeaderForService(service.getFinancial(), input.leaderId()));
        }
        if (input.meters() != null) {
            validatePositive(input.meters(), "meters");
            service.setMeters(input.meters());
        }
        BigDecimal unitPrice = zeroIfNull(service.getFinancial().getJvaPricePerMeter());
        validateNonNegative(unitPrice, "unitPrice");
        service.setUnitPrice(unitPrice);
        service.setGrossValue(service.getMeters().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
        if (input.notes() != null) service.setNotes(input.notes());
        if (input.startDate() != null) service.setStartDate(input.startDate());
        if (input.endDate() != null) service.setEndDate(input.endDate());
        if (input.days() != null) {
            validateDays(input.days());
            service.setDays(input.days());
        }

        if (service.getLeader() == null
                && zeroIfNull(service.getFinancial().getLeaderPricePerMeter()).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("leaderId is required when leaderPricePerMeter is greater than zero.");
        }

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

        PaymentCategory category = input.category() != null ? input.category() : payment.getCategory();
        Long effectiveEmployeeId;
        if (input.employeeId() != null) {
            effectiveEmployeeId = input.employeeId();
        } else {
            effectiveEmployeeId = payment.getEmployee() == null ? null : payment.getEmployee().getId();
        }
        FuncionariosModel employee = resolvePaymentEmployee(category, effectiveEmployeeId);

        if (input.paymentDate() != null) payment.setPaymentDate(input.paymentDate());
        if (input.name() != null && !input.name().isBlank()) payment.setName(input.name().trim());
        if (input.invoiceNumber() != null) payment.setInvoiceNumber(input.invoiceNumber());
        if (input.amount() != null) {
            validatePositive(input.amount(), "amount");
            payment.setAmount(input.amount());
        }
        payment.setCategory(category);
        if (input.notes() != null) payment.setNotes(input.notes());
        payment.setEmployee(employee);
        ClientModel client = payment.getClient();
        if (input.clientCnpj() != null) {
            client = resolveClient(input.clientCnpj());
        }
        if (category == PaymentCategory.CLIENT_PAYMENT && client == null) {
            client = payment.getFinancial().getPark().getClient();
        }
        payment.setClient(client);

        return paymentEntryRepository.save(payment);
    }

    @Transactional
    public void deletePaymentEntry(Long paymentId) {
        PaymentEntryModel payment = paymentEntryRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment entry not found for id " + paymentId));
        paymentEntryRepository.delete(payment);
    }

    @Transactional
    public PaymentEntryModel uploadPaymentReceipt(
            Long paymentId,
            String originalFilename,
            String contentType,
            byte[] data
    ) {
        PaymentEntryModel payment = paymentEntryRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment entry not found for id " + paymentId));

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Receipt file cannot be empty.");
        }
        if (data.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Receipt file cannot exceed 10MB.");
        }

        String normalizedContentType = normalizeReceiptContentType(contentType, originalFilename);
        String normalizedFileName = normalizeReceiptFileName(originalFilename, normalizedContentType, paymentId);

        payment.setReceiptBytes(data);
        payment.setReceiptFileName(normalizedFileName);
        payment.setReceiptContentType(normalizedContentType);
        payment.setReceiptSize((long) data.length);
        payment.setHasReceipt(true);

        return paymentEntryRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentReceiptFile getPaymentReceipt(Long paymentId) {
        PaymentEntryModel payment = paymentEntryRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Payment entry not found for id " + paymentId));

        if (!Boolean.TRUE.equals(payment.getHasReceipt())
                || payment.getReceiptBytes() == null
                || payment.getReceiptBytes().length == 0) {
            throw new NoSuchElementException("No receipt attached to this payment.");
        }

        String contentType = payment.getReceiptContentType() == null || payment.getReceiptContentType().isBlank()
                ? "application/octet-stream"
                : payment.getReceiptContentType();

        String fileName = normalizeReceiptFileName(payment.getReceiptFileName(), contentType, paymentId);
        return new PaymentReceiptFile(fileName, contentType, payment.getReceiptBytes());
    }

    private String normalizeReceiptContentType(String contentType, String fileName) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
        if (normalized.isEmpty() && fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".pdf")) normalized = "application/pdf";
            if (lowerName.endsWith(".png")) normalized = "image/png";
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) normalized = "image/jpeg";
            if (lowerName.endsWith(".webp")) normalized = "image/webp";
        }

        if (!"application/pdf".equals(normalized)
                && !"image/png".equals(normalized)
                && !"image/jpeg".equals(normalized)
                && !"image/webp".equals(normalized)) {
            throw new IllegalArgumentException("Only PDF or image files are accepted.");
        }
        return normalized;
    }

    private String normalizeReceiptFileName(String originalFilename, String contentType, Long paymentId) {
        String candidate = originalFilename == null ? "" : originalFilename.trim();
        if (candidate.isEmpty()) {
            String extension = switch (contentType) {
                case "application/pdf" -> ".pdf";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            return "payment-receipt-" + paymentId + extension;
        }
        return candidate.replaceAll("[\\r\\n\\\\/]+", "_");
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
            List<LeaderEarningSummary> leaderEarnings,
            BigDecimal taxValue,
            BigDecimal carRentalValue,
            BigDecimal clientPaymentsReceived,
            BigDecimal clientBalancePending,
            BigDecimal additionalPayments,
            BigDecimal totalCost,
            BigDecimal netRevenue,
            BigDecimal marginPercent
    ) {}

    public record LeaderEarningSummary(
            Long leaderId,
            String leaderName,
            BigDecimal totalMeters,
            BigDecimal rateUsed,
            BigDecimal totalEarnings
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

    public record CarRentalPeriodTotal(
            Long periodId,
            Long parkId,
            String parkName,
            Integer year,
            Integer month,
            BigDecimal value
    ) {}

    public record CarRentalMonthTotal(
            Integer year,
            Integer month,
            BigDecimal total
    ) {}

    public record CarRentalYearTotal(
            Integer year,
            BigDecimal total
    ) {}

    public record CarRentalSummary(
            Long parkId,
            String parkName,
            BigDecimal totalAllTime,
            BigDecimal currentYearTotal,
            List<CarRentalYearTotal> annualTotals,
            List<CarRentalMonthTotal> monthlyTotals,
            List<CarRentalPeriodTotal> periodTotals
    ) {}

    public record PaymentReceiptFile(
            String fileName,
            String contentType,
            byte[] data
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
            Long leaderId,
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
            String notes,
            Long employeeId,
            String clientCnpj
    ) {}
}
