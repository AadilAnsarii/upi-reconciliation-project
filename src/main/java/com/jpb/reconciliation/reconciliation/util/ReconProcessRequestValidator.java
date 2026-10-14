package com.jpb.reconciliation.reconciliation.util;


import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.jpb.reconciliation.reconciliation.dto.ReconProcessRequest;

@Component
public class ReconProcessRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ReconProcessRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ReconProcessRequest request = (ReconProcessRequest) target;
        
        Long inputCount = request.getInputCount();
        
        if (inputCount == null) {
            return; // Will be caught by @NotNull validation
        }
        
        // Validate File Type Mappings count
        if (request.getFileTypeMappings() != null) {
            if (request.getFileTypeMappings().size() > inputCount) {
                errors.rejectValue("fileTypeMappings", 
                    "fileTypeMappings.count.exceeded", 
                    "Number of file type mappings (" + request.getFileTypeMappings().size() + 
                    ") cannot exceed input count (" + inputCount + ")");
            }
            
            if (request.getFileTypeMappings().size() < inputCount) {
                errors.rejectValue("fileTypeMappings", 
                    "fileTypeMappings.count.insufficient", 
                    "Number of file type mappings (" + request.getFileTypeMappings().size() + 
                    ") should match input count (" + inputCount + ")");
            }
        } else {
            errors.rejectValue("fileTypeMappings", 
                "fileTypeMappings.required", 
                "File type mappings are required");
        }
        
        // Validate Template Mappings count
        if (request.getTemplateMappings() != null) {
            if (request.getTemplateMappings().size() > inputCount) {
                errors.rejectValue("templateMappings", 
                    "templateMappings.count.exceeded", 
                    "Number of template mappings (" + request.getTemplateMappings().size() + 
                    ") cannot exceed input count (" + inputCount + ")");
            }
            
            if (request.getTemplateMappings().size() < inputCount) {
                errors.rejectValue("templateMappings", 
                    "templateMappings.count.insufficient", 
                    "Number of template mappings (" + request.getTemplateMappings().size() + 
                    ") should match input count (" + inputCount + ")");
            }
        } else {
            errors.rejectValue("templateMappings", 
                "templateMappings.required", 
                "Template mappings are required");
        }
        
        // Validate Matching Fields count
        if (request.getMatchingFields() != null) {
            if (request.getMatchingFields().size() > inputCount) {
                errors.rejectValue("matchingFields", 
                    "matchingFields.count.exceeded", 
                    "Number of matching fields (" + request.getMatchingFields().size() + 
                    ") cannot exceed input count (" + inputCount + ")");
            }
            
            if (request.getMatchingFields().size() < inputCount) {
                errors.rejectValue("matchingFields", 
                    "matchingFields.count.insufficient", 
                    "Number of matching fields (" + request.getMatchingFields().size() + 
                    ") should match input count (" + inputCount + ")");
            }
        } else {
            errors.rejectValue("matchingFields", 
                "matchingFields.required", 
                "Matching fields are required");
        }
        
        // Validate that file type numbers are sequential (1, 2, 3...)
        if (request.getFileTypeMappings() != null) {
            for (int i = 0; i < request.getFileTypeMappings().size(); i++) {
                ReconProcessRequest.FileTypeMapping mapping = request.getFileTypeMappings().get(i);
                if (mapping.getFileTypeNumber() != (i + 1)) {
                    errors.rejectValue("fileTypeMappings[" + i + "].fileTypeNumber", 
                        "fileTypeMappings.number.invalid", 
                        "File type numbers must be sequential starting from 1");
                }
            }
        }
        
        // Validate that template numbers are sequential (1, 2, 3...)
        if (request.getTemplateMappings() != null) {
            for (int i = 0; i < request.getTemplateMappings().size(); i++) {
                ReconProcessRequest.TemplateMapping mapping = request.getTemplateMappings().get(i);
                if (mapping.getTemplateNumber() != (i + 1)) {
                    errors.rejectValue("templateMappings[" + i + "].templateNumber", 
                        "templateMappings.number.invalid", 
                        "Template numbers must be sequential starting from 1");
                }
            }
        }
        
        // Validate that matching field numbers are sequential (1, 2, 3...)
        if (request.getMatchingFields() != null) {
            for (int i = 0; i < request.getMatchingFields().size(); i++) {
                ReconProcessRequest.MatchingFieldMapping mapping = request.getMatchingFields().get(i);
                if (mapping.getFieldNumber() != (i + 1)) {
                    errors.rejectValue("matchingFields[" + i + "].fieldNumber", 
                        "matchingFields.number.invalid", 
                        "Matching field numbers must be sequential starting from 1");
                }
            }
        }
    }
}