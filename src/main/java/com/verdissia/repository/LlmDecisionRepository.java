package com.verdissia.repository;

import com.verdissia.model.LlmDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LlmDecisionRepository extends JpaRepository<LlmDecision, Long> {
    
    List<LlmDecision> findByContratId(Long contratId);
    
    List<LlmDecision> findByDecision(String decision);
    
    List<LlmDecision> findByProcessStatus(LlmDecision.ProcessStatus processStatus);
    
    Optional<LlmDecision> findTopByContratIdOrderByProcessedAtDesc(Long contratId);
}
