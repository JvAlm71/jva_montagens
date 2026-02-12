package com.java10x.jvaMontagens.controller;

import com.java10x.jvaMontagens.model.*;
import com.java10x.jvaMontagens.service.FinancialService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/financial")
public class Financial {
    private final FinancialService financialService;

    public Financial(FinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/status")
    public String getStatus() {
        return "Financial service is running.";
    }

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialModel createPeriod(@RequestBody CreateFinancialPeriodRequest request) {
        try {
            return financialService.createFinancialPeriod(
                    new FinancialService.CreateFinancialPeriodInput(
                            request.parkId(),
                            request.year(),
                            request.month(),
                            request.jvaPricePerMeter(),
                            request.leaderPricePerMeter(),
                            request.taxRate(),
                            request.carRentalValue(),
                            request.status(),
                            request.administratorId()
                    )
            );
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/periods")
    public List<FinancialModel> listPeriods(@RequestParam(required = false) Long parkId) {
        return financialService.listPeriods(parkId);
    }

    @GetMapping("/periods/{periodId}")
    public FinancialModel getPeriod(@PathVariable Long periodId) {
        try {
            return financialService.getPeriod(periodId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/periods/{periodId}")
    public FinancialModel updatePeriod(@PathVariable Long periodId, @RequestBody UpdateFinancialPeriodRequest request) {
        try {
            return financialService.updatePeriod(periodId, new FinancialService.UpdateFinancialPeriodInput(
                    request.jvaPricePerMeter(),
                    request.leaderPricePerMeter(),
                    request.taxRate(),
                    request.carRentalValue(),
                    request.status()
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/periods/{periodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePeriod(@PathVariable Long periodId) {
        try {
            financialService.deletePeriod(periodId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/periods/{periodId}/services")
    public List<ServiceEntryModel> listServices(@PathVariable Long periodId) {
        try {
            return financialService.listServiceEntries(periodId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/services/{serviceId}")
    public ServiceEntryModel updateService(@PathVariable Long serviceId, @RequestBody UpdateServiceEntryRequest request) {
        try {
            return financialService.updateServiceEntry(serviceId, new FinancialService.UpdateServiceEntryInput(
                    request.serviceType(),
                    request.teamType(),
                    request.meters(),
                    request.unitPrice(),
                    request.grossValue(),
                    request.notes(),
                    request.startDate(),
                    request.endDate(),
                    request.days()
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable Long serviceId) {
        try {
            financialService.deleteServiceEntry(serviceId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/periods/{periodId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceEntryModel addService(
            @PathVariable Long periodId,
            @RequestBody CreateServiceEntryRequest request
    ) {
        try {
            List<FinancialService.ServiceHelperInput> helpers = request.helpers() == null
                    ? List.of()
                    : request.helpers().stream()
                    .map(helper -> new FinancialService.ServiceHelperInput(
                            helper.employeeId(),
                            helper.dailyRateUsed(),
                            helper.daysUsed(),
                            helper.totalCost()
                    ))
                    .toList();

            return financialService.addServiceEntry(
                    periodId,
                    new FinancialService.CreateServiceEntryInput(
                            request.serviceType(),
                            request.teamType(),
                            request.leaderId(),
                            request.meters(),
                            request.unitPrice(),
                            request.grossValue(),
                            request.notes(),
                            request.startDate(),
                            request.endDate(),
                            request.days(),
                            helpers
                    )
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/periods/{periodId}/payments")
    public List<PaymentEntryModel> listPayments(@PathVariable Long periodId) {
        try {
            return financialService.listPaymentEntries(periodId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/payments/{paymentId}")
    public PaymentEntryModel updatePayment(@PathVariable Long paymentId, @RequestBody UpdatePaymentEntryRequest request) {
        try {
            return financialService.updatePaymentEntry(paymentId, new FinancialService.UpdatePaymentEntryInput(
                    request.paymentDate(),
                    request.name(),
                    request.invoiceNumber(),
                    request.amount(),
                    request.category(),
                    request.notes()
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Long paymentId) {
        try {
            financialService.deletePaymentEntry(paymentId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/periods/{periodId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentEntryModel addPayment(
            @PathVariable Long periodId,
            @RequestBody CreatePaymentEntryRequest request
    ) {
        try {
            return financialService.addPaymentEntry(
                    periodId,
                    new FinancialService.CreatePaymentEntryInput(
                            request.paymentDate(),
                            request.name(),
                            request.invoiceNumber(),
                            request.amount(),
                            request.category(),
                            request.notes(),
                            request.employeeId(),
                            request.clientCnpj()
                    )
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/periods/{periodId}/summary")
    public FinancialService.FinancialSummary summary(@PathVariable Long periodId) {
        try {
            return financialService.calculateSummary(periodId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/parks/{parkId}/overview")
    public FinancialService.ParkFinancialOverview parkOverview(@PathVariable Long parkId) {
        try {
            return financialService.calculateParkOverview(parkId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record CreateFinancialPeriodRequest(
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

    public record CreateServiceHelperRequest(
            Long employeeId,
            BigDecimal dailyRateUsed,
            Integer daysUsed,
            BigDecimal totalCost
    ) {}

    public record CreateServiceEntryRequest(
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
            List<CreateServiceHelperRequest> helpers
    ) {}

    public record CreatePaymentEntryRequest(
            LocalDate paymentDate,
            String name,
            String invoiceNumber,
            BigDecimal amount,
            PaymentCategory category,
            String notes,
            Long employeeId,
            String clientCnpj
    ) {}

    public record UpdateFinancialPeriodRequest(
            BigDecimal jvaPricePerMeter,
            BigDecimal leaderPricePerMeter,
            BigDecimal taxRate,
            BigDecimal carRentalValue,
            FinancialStatus status
    ) {}

    public record UpdateServiceEntryRequest(
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

    public record UpdatePaymentEntryRequest(
            LocalDate paymentDate,
            String name,
            String invoiceNumber,
            BigDecimal amount,
            PaymentCategory category,
            String notes
    ) {}
}
