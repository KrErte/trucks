package eu.fuelfleet.customer.dto;

import eu.fuelfleet.customer.entity.Customer;
import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerResponse {
    private UUID id;
    private String name;
    private String vatNumber;
    private String regCode;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String contactPerson;
    private Integer paymentTermDays;
    private boolean active;
    private String notes;

    public static CustomerResponse from(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId()).name(c.getName()).vatNumber(c.getVatNumber())
                .regCode(c.getRegCode()).email(c.getEmail()).phone(c.getPhone())
                .address(c.getAddress()).country(c.getCountry())
                .contactPerson(c.getContactPerson())
                .paymentTermDays(c.getPaymentTermDays())
                .active(c.isActive()).notes(c.getNotes())
                .build();
    }
}
