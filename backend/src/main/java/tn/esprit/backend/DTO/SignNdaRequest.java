package tn.esprit.backend.DTO;

import lombok.Data;
import tn.esprit.backend.enums.SignatureType;

@Data
public class SignNdaRequest {
    private String signerFullName;
    private SignatureType signatureType;
    private String signatureImageBase64;
    private String typedSignature;
    private boolean acceptedTerms;
}
