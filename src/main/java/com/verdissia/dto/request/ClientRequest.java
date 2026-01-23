package com.verdissia.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String voie;
    private String codePostal;
    private String ville;
}