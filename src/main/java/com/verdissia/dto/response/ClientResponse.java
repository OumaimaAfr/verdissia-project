package com.verdissia.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String voie;
    private String codePostal;
    private String ville;
}