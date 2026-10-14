package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.Param;


@Repository
public interface ParamRepo extends JpaRepository<Param, Long> {

    Param findByParamNameAndActiveYn(String paramName, String activeYn);
}