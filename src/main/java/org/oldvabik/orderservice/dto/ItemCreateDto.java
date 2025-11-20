package org.oldvabik.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateDto {
    @NotBlank
    @Size(min = 3, max = 32)
    private String name;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;
}