package com.verdissia.repository;

import com.verdissia.model.Offre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Integer> {

    List<Offre> findByTypeEnergieAndPreferenceOffreIgnoreCase(Offre.TypeEnergie typeEnergie, String preferenceOffre);
}
