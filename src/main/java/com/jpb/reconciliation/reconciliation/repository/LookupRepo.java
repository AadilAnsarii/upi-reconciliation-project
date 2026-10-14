package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.Lookup;



@Repository
public interface LookupRepo extends JpaRepository<Lookup, Long> {

    // Name + Active se dhundho
    List<Lookup> findByLookupNameAndActiveYn(String lookupName, String activeYn);

    // Sirf active wale sab lo
    List<Lookup> findByActiveYn(String activeYn);

    // Code duplicate check ke liye
    List<Lookup> findByLookupCodeAndActiveYn(String lookupCode, String activeYn);

    // Grouped API ke liye — saare distinct LOOKUP_NAME lo
    @Query("SELECT DISTINCT l.lookupName FROM Lookup l WHERE l.activeYn = 'Y'")
    List<String> findDistinctLookupNames();
}