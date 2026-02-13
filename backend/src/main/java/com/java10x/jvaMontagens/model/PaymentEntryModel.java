package com.java10x.jvaMontagens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_entries")
public class PaymentEntryModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_id", nullable = false)
    private FinancialModel financial;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private String name;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentCategory category = PaymentCategory.OTHER;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id")
    private FuncionariosModel employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_cnpj", referencedColumnName = "cnpj")
    private ClientModel client;

    @Column(name = "has_receipt")
    private Boolean hasReceipt = false;

    @Column(name = "receipt_file_name", length = 255)
    private String receiptFileName;

    @Column(name = "receipt_content_type", length = 120)
    private String receiptContentType;

    @Column(name = "receipt_size")
    private Long receiptSize;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "receipt_bytes", columnDefinition = "bytea")
    private byte[] receiptBytes;
}
