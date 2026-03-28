package eu.fuelfleet.customer.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CustomerRequest {
    private String name;
    private String vatNumber;
    private String regCode;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String contactPerson;
    private Integer paymentTermDays;
    private String notes;
}
