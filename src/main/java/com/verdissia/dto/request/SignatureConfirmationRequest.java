package com.verdissia.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureConfirmationRequest {
    
    @NotBlank(message = "L'ID de la demande est obligatoire")
    private String idDemande;
    
    @NotBlank(message = "Le statut de signature est obligatoire")
    private String statutSignature;
}