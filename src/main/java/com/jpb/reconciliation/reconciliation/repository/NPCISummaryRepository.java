package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.NPCISummaryEntity;

@Repository
public interface NPCISummaryRepository extends JpaRepository<NPCISummaryEntity, Long>{

	List<NPCISummaryEntity> findByNpciFileDate(String formattedDate);

	List<NPCISummaryEntity> findByNpciFileDateAndProductType(String formattedDate, String productType);

}
