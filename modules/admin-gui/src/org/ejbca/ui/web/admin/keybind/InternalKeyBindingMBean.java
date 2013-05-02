/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.admin.keybind;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.authorization.control.AccessControlSessionLocal;
import org.cesecore.authorization.control.CryptoTokenRules;
import org.cesecore.certificates.ca.CA;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.certificates.util.AlgorithmTools;
import org.cesecore.keys.token.CryptoTokenInfo;
import org.cesecore.keys.token.CryptoTokenManagementSessionLocal;
import org.cesecore.keys.token.CryptoTokenOfflineException;
import org.cesecore.util.CertTools;
import org.ejbca.core.ejb.signer.CertificateImportException;
import org.ejbca.core.ejb.signer.InternalKeyBinding;
import org.ejbca.core.ejb.signer.InternalKeyBindingInfo;
import org.ejbca.core.ejb.signer.InternalKeyBindingMgmtSessionLocal;
import org.ejbca.core.ejb.signer.InternalKeyBindingNameInUseException;
import org.ejbca.core.ejb.signer.InternalKeyBindingProperty;
import org.ejbca.core.ejb.signer.InternalKeyBindingRules;
import org.ejbca.core.ejb.signer.InternalKeyBindingStatus;
import org.ejbca.ui.web.admin.BaseManagedBean;
import org.jboss.logging.Logger;

/**
 * JavaServer Faces Managed Bean for managing InternalKeyBindings.
 * Session scoped and will cache the list of tokens and keys.
 * 
 * @version $Id$
 */
public class InternalKeyBindingMBean extends BaseManagedBean implements Serializable {

    public class GuiInfo {
        private final int internalKeyBindingId;
        private final String name;
        private final int cryptoTokenId;
        private final String cryptoTokenName;
        private final boolean cryptoTokenAvailable;
        private final boolean cryptoTokenActive;
        private final String keyPairAlias;
        private final String nextKeyPairAlias;
        private final String status;
        private final String certificateId;
        private final String certificateIssuerDn;
        private final String certificateInternalCaName;
        private final int certificateInternalCaId;
        private final String certificateSerialNumber;

        private GuiInfo(int internalKeyBindingId, String name, int cryptoTokenId, String cryptoTokenName, boolean cryptoTokenAvailable,
                boolean cryptoTokenActive, String keyPairAlias, String nextKeyPairAlias, String status, String certificateId,
                String certificateIssuerDn, String certificateInternalCaName, int certificateInternalCaId, String certificateSerialNumber) {
            this.internalKeyBindingId = internalKeyBindingId;
            this.name = name;
            this.cryptoTokenId = cryptoTokenId;
            this.cryptoTokenName = cryptoTokenName;
            this.cryptoTokenAvailable = cryptoTokenAvailable;
            this.cryptoTokenActive = cryptoTokenActive;
            this.keyPairAlias = keyPairAlias;
            this.nextKeyPairAlias = nextKeyPairAlias;
            this.status = status;
            this.certificateId = certificateId;
            this.certificateIssuerDn = certificateIssuerDn;
            this.certificateInternalCaName = certificateInternalCaName;
            this.certificateInternalCaId = certificateInternalCaId;
            this.certificateSerialNumber = certificateSerialNumber;
        }

        public int getInternalKeyBindingId() { return internalKeyBindingId; }
        public String getName() { return name; }
        public int getCryptoTokenId() { return cryptoTokenId; }
        public String getCryptoTokenName() { return cryptoTokenName; }
        public String getKeyPairAlias() { return keyPairAlias; }
        public String getNextKeyPairAlias() {return nextKeyPairAlias; }
        public String getStatus() { return status; }
        public String getCertificateId() { return certificateId; }
        public String getCertificateIssuerDn() { return certificateIssuerDn; }
        public String getCertificateInternalCaName() { return certificateInternalCaName; }
        public int getCertificateInternalCaId() { return certificateInternalCaId; }
        public String getCertificateSerialNumber() { return certificateSerialNumber; }

        public boolean isCertificateBound() { return certificateId != null; }
        public boolean isIssuedByInternalCa() { return getCertificateInternalCaName() != null; }
        public boolean isNextKeyAliasAvailable() { return nextKeyPairAlias != null; }
        public boolean isCryptoTokenAvailable() { return cryptoTokenAvailable; }
        public boolean isCryptoTokenActive() { return cryptoTokenActive; }
    }
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(InternalKeyBindingMBean.class);

    private final CryptoTokenManagementSessionLocal cryptoTokenManagementSession = getEjbcaWebBean().getEjb().getCryptoTokenManagementSession();
    private final InternalKeyBindingMgmtSessionLocal internalKeyBindingSession = getEjbcaWebBean().getEjb().getInternalKeyBindingMgmtSession();
    private final AccessControlSessionLocal accessControlSession = getEjbcaWebBean().getEjb().getAccessControlSession();
    private final AuthenticationToken authenticationToken = getAdmin();
    private final CertificateStoreSessionLocal certificateStoreSession = getEjbcaWebBean().getEjb().getCertificateStoreSession();
    private final CaSessionLocal caSession = getEjbcaWebBean().getEjb().getCaSession();

    private String selectedInternalKeyBindingType = null;
    private ListDataModel internalKeyBindingGuiList = null;

    public String getSelectedInternalKeyBindingType() {
        final String typeHttpParam = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getParameter("type");
        // First, check if the user has requested a valid type
        if (typeHttpParam != null && getAvailableKeyBindingTypes().contains(typeHttpParam)) {
            // The requested type is an existing type. Check if this is a change from the current value.
            //if (!typeHttpParam.equals(selectedInternalKeyBindingType)) {
                // Flush caches so we reload the page content
                flushListCaches();
            //}
            selectedInternalKeyBindingType = typeHttpParam;
        }
        if (selectedInternalKeyBindingType == null) {
            // If no type was requested, we use the first available type as default
            selectedInternalKeyBindingType = getAvailableKeyBindingTypes().get(0);
        }
        return selectedInternalKeyBindingType;
    }

    public List<String> getAvailableKeyBindingTypes() {
        final List<String> availableKeyBindingTypes = new ArrayList<String>();
        for (String current : internalKeyBindingSession.getAvailableTypesAndProperties(authenticationToken).keySet()) {
            availableKeyBindingTypes.add(current);
        }
        return availableKeyBindingTypes;
    }

    private void flushListCaches() {
        internalKeyBindingGuiList = null;
    }

    public ListDataModel getInternalKeyBindingGuiList() {
        if (internalKeyBindingGuiList==null) {
            // Get the current type of tokens we operate on
            final String internalKeyBindingType = getSelectedInternalKeyBindingType();
            List<GuiInfo> internalKeyBindingList = new LinkedList<GuiInfo>();
            for (InternalKeyBindingInfo current : internalKeyBindingSession.getInternalKeyBindingInfos(authenticationToken, internalKeyBindingType)) {
                final int cryptoTokenId = current.getCryptoTokenId();
                final CryptoTokenInfo cryptoTokenInfo = cryptoTokenManagementSession.getCryptoTokenInfo(cryptoTokenId);
                final String cryptoTokenName;
                boolean cryptoTokenAvailable = false;
                boolean cryptoTokenActive = false;
                if (cryptoTokenInfo == null) {
                    cryptoTokenName = "unknown";
                } else {
                    cryptoTokenAvailable = accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.GENERATE_KEYS.resource()+"/"+cryptoTokenId);
                    cryptoTokenActive = cryptoTokenInfo.isActive();
                    cryptoTokenName = cryptoTokenInfo.getName();
                }
                final String certificateId = current.getCertificateId();
                final Certificate certificate = certificateId == null ? null : certificateStoreSession.findCertificateByFingerprint(certificateId);
                String certificateIssuerDn = "";
                String certificateSerialNumber = "";
                String certificateInternalCaName = null;
                int certificateInternalCaId = certificateIssuerDn.hashCode();
                if (certificate != null) {
                    certificateIssuerDn = CertTools.getIssuerDN(certificate);
                    certificateSerialNumber = CertTools.getSerialNumberAsString(certificate);
                    try {
                        // Note that we can do lookups using the .hashCode, but we will use the objects id
                        final CA ca = caSession.getCA(authenticationToken, certificateIssuerDn.hashCode());
                        certificateInternalCaName = ca.getName();
                        certificateInternalCaId = ca.getCAId();
                    } catch (Exception e) {
                        // CADoesntExistsException or AuthorizationDeniedException
                        // The CA is for the purpose of "internal" renewal not available to this administrator.
                    }
                }
                internalKeyBindingList.add(new GuiInfo(current.getId(), current.getName(), cryptoTokenId, cryptoTokenName,
                        cryptoTokenAvailable, cryptoTokenActive, current.getKeyPairAlias(), current.getNextKeyPairAlias(),
                        current.getStatus().name(), current.getCertificateId(), certificateIssuerDn, certificateInternalCaName,
                        certificateInternalCaId, certificateSerialNumber));
                Collections.sort(internalKeyBindingList, new Comparator<GuiInfo>() {
                    @Override
                    public int compare(final GuiInfo guiInfo1, final GuiInfo guiInfo2) {
                        return guiInfo1.getName().compareToIgnoreCase(guiInfo2.getName());
                    }
                });
            }
            internalKeyBindingGuiList = new ListDataModel(internalKeyBindingList);
        }
        // View the list will purge the view cache
        flushSingleViewCache();
        return internalKeyBindingGuiList;
    }
    
    public void commandRenewCertificate() {
        try {
            final GuiInfo guiInfo = (GuiInfo) internalKeyBindingGuiList.getRowData();
            final int internalKeyBindingId = guiInfo.getInternalKeyBindingId();
            final String certificateId = internalKeyBindingSession.renewInternallyIssuedCertificate(authenticationToken, internalKeyBindingId);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("New certificate with fingerprint " + certificateId + " has been issed."));
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CertificateImportException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CryptoTokenOfflineException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
        flushListCaches();
    }
    public void commandReloadCertificate() {
        try {
            final GuiInfo guiInfo = (GuiInfo) internalKeyBindingGuiList.getRowData();
            final int internalKeyBindingId = guiInfo.getInternalKeyBindingId();
            final String certificateId = internalKeyBindingSession.updateCertificateForInternalKeyBinding(authenticationToken, internalKeyBindingId);
            if (certificateId == null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("No new certificate for " + guiInfo.getName() + "."));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("New certificate found for " + guiInfo.getName() + "."));
            }
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CertificateImportException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CryptoTokenOfflineException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
        flushListCaches();
    }

    public void commandGenerateNewKey() {
        try {
            final GuiInfo guiInfo = (GuiInfo) internalKeyBindingGuiList.getRowData();
            final int internalKeyBindingId = guiInfo.getInternalKeyBindingId();
            final String nextKeyPairAlias = internalKeyBindingSession.generateNextKeyPair(authenticationToken, internalKeyBindingId);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Generated next key with alias " + nextKeyPairAlias + "."));
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CryptoTokenOfflineException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (InvalidKeyException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (InvalidAlgorithmParameterException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
        flushListCaches();
    }

    public void commandGenerateRequest() {
        try {
            final GuiInfo guiInfo = (GuiInfo) internalKeyBindingGuiList.getRowData();
            final int internalKeyBindingId = guiInfo.getInternalKeyBindingId();
            final byte[] pkcs10 = internalKeyBindingSession.generateCsrForNextKey(authenticationToken, internalKeyBindingId);
            final byte[] pemEncodedPkcs10 = CertTools.getPEMFromCertificateRequest(pkcs10);
            final HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            final OutputStream outputStream = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename=\""+guiInfo.getName()+".pkcs10.pem"+"\"");
            outputStream.flush();
            outputStream.write(pemEncodedPkcs10);
            outputStream.close();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CryptoTokenOfflineException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    public void commandDisable() {
        changeStatus(((GuiInfo) internalKeyBindingGuiList.getRowData()).getInternalKeyBindingId(), InternalKeyBindingStatus.DISABLED);
        flushListCaches();
    }

    public void commandEnable() {
        changeStatus(((GuiInfo) internalKeyBindingGuiList.getRowData()).getInternalKeyBindingId(), InternalKeyBindingStatus.ACTIVE);
        flushListCaches();
    }
    
    private void changeStatus(final int internalKeyBindingId, final InternalKeyBindingStatus internalKeyBindingStatus) {
        try {
            final InternalKeyBinding internalKeyBinding = internalKeyBindingSession.getInternalKeyBinding(authenticationToken, internalKeyBindingId);
            if (internalKeyBinding.getCertificateId() == null && internalKeyBindingStatus.equals(InternalKeyBindingStatus.ACTIVE)) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Cannot activate InternalKeyBinding that has no certificate.", null));
            } else {
                internalKeyBinding.setStatus(internalKeyBindingStatus);
                internalKeyBindingSession.persistInternalKeyBinding(authenticationToken, internalKeyBinding);
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(internalKeyBinding.getName() + " status is now " + internalKeyBindingStatus.name()));
            }
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (InternalKeyBindingNameInUseException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
        flushListCaches();
    }
    
    public void commandDelete() {
        try {
            final GuiInfo guiInfo = (GuiInfo) internalKeyBindingGuiList.getRowData();
            if (internalKeyBindingSession.deleteInternalKeyBinding(authenticationToken, guiInfo.getInternalKeyBindingId())) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(guiInfo.getName() + " deleted."));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(guiInfo.getName() + " had already been deleted."));
            }
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
        flushListCaches();
    }
    
    private void flushSingleViewCache() {
        currentInternalKeyBindingId = null;
        currentName = null;
        currentCryptoToken = null;
        currentKeyPairAlias = null;
        currentSignatureAlgorithm = null;
        currentNextKeyPairAlias = null;
        internalKeyBindingPropertyList = null;
        inEditMode = false;
    }
    
    private String currentInternalKeyBindingId = null;
    public String getCurrentInternalKeyBindingId() {
        final String idHttpParam = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getParameter("internalKeyBindingId");
        boolean changed = false;
        // First, check if the user has requested a valid type
        if (idHttpParam != null && isInteger(idHttpParam)) {
            // The requested type is an existing type. Check if this is a change from the current value.
            if (!idHttpParam.equals(currentInternalKeyBindingId)) {
                // Flush caches so we reload the page content
                changed = true;
            }
            currentInternalKeyBindingId = idHttpParam;
        }
        if (currentInternalKeyBindingId == null) {
            // If no valid id was requested, we assume that a new one should be created
            currentInternalKeyBindingId = "0";
            changed = true;
        }
        if (changed) {
            if ("0".equals(currentInternalKeyBindingId)) {
                inEditMode = true;
            }
            flushCurrentCache();
        }
        return currentInternalKeyBindingId;
    }
    
    private boolean isInteger(final String input) {
        try {
            Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
    
    private void flushCurrentCache() {
        if ("0".equals(currentInternalKeyBindingId)) {
            // Show defaults for a new object
            currentName = "New " + getSelectedInternalKeyBindingType();
            getAvailableCryptoTokens();
            getAvailableKeyPairAliases();
            getAvailableSignatureAlgorithms();
            internalKeyBindingPropertyList = new ListDataModel(internalKeyBindingSession.getAvailableTypesAndProperties(authenticationToken).get(getSelectedInternalKeyBindingType()));
        } else {
            // Load existing
            final int internalKeyBindingId = Integer.parseInt(currentInternalKeyBindingId);
            final InternalKeyBinding internalKeyBinding;
            try {
                internalKeyBinding = internalKeyBindingSession.getInternalKeyBinding(authenticationToken, internalKeyBindingId);
            } catch (AuthorizationDeniedException e) {
                // No longer authorized to this token, or the user tried to pull a fast one
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
                return;
            }
            currentName = internalKeyBinding.getName();
            currentCryptoToken = internalKeyBinding.getCryptoTokenId();
            currentKeyPairAlias = internalKeyBinding.getKeyPairAlias();
            currentSignatureAlgorithm = internalKeyBinding.getSignatureAlgorithm();
            currentNextKeyPairAlias = internalKeyBinding.getNextKeyPairAlias();
            internalKeyBindingPropertyList = new ListDataModel(internalKeyBinding.getCopyOfProperties());
        }
    }

    private boolean inEditMode = false;
    
    /** @return true for any InternalKeyBinding where the user is authorized to edit */
    public boolean isSwitchToEditAllowed() {
        return !inEditMode && accessControlSession.isAuthorizedNoLogging(authenticationToken,
                InternalKeyBindingRules.MODIFY.resource() + "/" + getCurrentInternalKeyBindingId());
    }

    /** @return true for any InternalKeyBinding except new id="0" */
    public boolean isSwitchToViewAllowed() {
        return inEditMode && !"0".equals(getCurrentInternalKeyBindingId());
    }

    public boolean isInEditMode() {
        return inEditMode;
    }

    public boolean isCryptoTokenActive() {
        final CryptoTokenInfo cryptoTokenInfo = cryptoTokenManagementSession.getCryptoTokenInfo(currentCryptoToken);
        return cryptoTokenInfo==null || cryptoTokenInfo.isActive();
    }

    public void switchToEdit() {
        inEditMode = true;
    }

    public void switchToView() {
        inEditMode = false;
    }

    private String currentName = null;
    private Integer currentCryptoToken = null;
    private String currentKeyPairAlias = null;
    private String currentSignatureAlgorithm = null;
    private String currentNextKeyPairAlias = null;
    private ListDataModel internalKeyBindingPropertyList = null;
    
    public boolean isCreatingNew() {
        return "0".equals(getCurrentInternalKeyBindingId());
    }

    public Integer getCurrentCryptoToken() { return currentCryptoToken; }
    public void setCurrentCryptoToken(Integer currentCryptoToken) {
        if (currentCryptoToken != null && !currentCryptoToken.equals(this.currentCryptoToken)) {
            // Clear if we change CryptoToken
            currentKeyPairAlias = null;
            currentSignatureAlgorithm = null;
            currentNextKeyPairAlias =  null;
        }
        this.currentCryptoToken = currentCryptoToken;
    }
    public String getCurrentCryptoTokenName() {
        if (currentCryptoToken == null) {
            final List<SelectItem> availableCryptoTokens = getAvailableCryptoTokens();
            if (availableCryptoTokens.isEmpty()) {
                return null;
            } else {
                currentCryptoToken = (Integer) availableCryptoTokens.get(0).getValue();
            }
        }
        return cryptoTokenManagementSession.getCryptoTokenInfo(currentCryptoToken.intValue()).getName();
    }

    public String getCurrentName() { return currentName; }
    public void setCurrentName(String currentName) { this.currentName = currentName; }
    public String getCurrentKeyPairAlias() { return currentKeyPairAlias; }
    public void setCurrentKeyPairAlias(String currentKeyPairAlias) {
        if (currentKeyPairAlias != null && !currentKeyPairAlias.equals(this.currentKeyPairAlias)) {
            // Clear if we change CryptoToken
            currentSignatureAlgorithm = null;
        }
        this.currentKeyPairAlias = currentKeyPairAlias;
    }
    public String getCurrentSignatureAlgorithm() { return currentSignatureAlgorithm; }
    public void setCurrentSignatureAlgorithm(String currentSignatureAlgorithm) { this.currentSignatureAlgorithm = currentSignatureAlgorithm; }
    public String getCurrentNextKeyPairAlias() { return currentNextKeyPairAlias; }
    public void setCurrentNextKeyPairAlias(String currentNextKeyPairAlias) { this.currentNextKeyPairAlias = currentNextKeyPairAlias; }

    public List<SelectItem/*<Integer,String>*/> getAvailableCryptoTokens() {
        final List<SelectItem> availableCryptoTokens = new ArrayList<SelectItem>();
        for (CryptoTokenInfo current : cryptoTokenManagementSession.getCryptoTokenInfos(authenticationToken)) {
            if (current.isActive() && accessControlSession.isAuthorizedNoLogging(authenticationToken, CryptoTokenRules.USE.resource() + "/" + current.getCryptoTokenId())) {
                availableCryptoTokens.add(new SelectItem(current.getCryptoTokenId(), current.getName()));
            }
        }
        if (!availableCryptoTokens.isEmpty() && currentCryptoToken == null) {
            currentCryptoToken = (Integer) availableCryptoTokens.get(0).getValue();
        }
        return availableCryptoTokens;
    }

    public void reloadCryptoToken() {
        setCurrentKeyPairAlias((String) getAvailableKeyPairAliases().get(0).getValue());
        setCurrentSignatureAlgorithm((String) getAvailableSignatureAlgorithms().get(0).getValue());
    }
    public void reloadKeyPairAlias() {
        setCurrentSignatureAlgorithm((String) getAvailableSignatureAlgorithms().get(0).getValue());
    }
    
    public List<SelectItem/*<String,String>*/> getAvailableKeyPairAliases() {
        final List<SelectItem> availableKeyPairAliases = new ArrayList<SelectItem>();
        try {
            if (currentCryptoToken != null) {
                for (final String alias : cryptoTokenManagementSession.getKeyPairAliases(authenticationToken, currentCryptoToken.intValue())) {
                    availableKeyPairAliases.add(new SelectItem(alias, alias));
                }
                if (currentKeyPairAlias == null && !availableKeyPairAliases.isEmpty()) {
                    currentKeyPairAlias = (String) availableKeyPairAliases.get(0).getValue();
                }
                if (currentSignatureAlgorithm == null) {
                    final List<SelectItem> availableSignatureAlgorithms = getAvailableSignatureAlgorithms();
                    if (!availableSignatureAlgorithms.isEmpty()) {
                        currentSignatureAlgorithm = (String) availableSignatureAlgorithms.get(0).getValue();
                    }
                }
            }
        } catch (Exception e) {
            // No longer active (CryptoTokenOfflineException) or No longer authorized (AuthorizationDeniedException)
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
            currentCryptoToken = null;
            currentKeyPairAlias = null;
            currentNextKeyPairAlias = null;
        }
        return availableKeyPairAliases;
    }
    
    public List<SelectItem/*<String,String>*/> getAvailableSignatureAlgorithms() {
        final List<SelectItem> availableSignatureAlgorithms = new ArrayList<SelectItem>();
        if (currentCryptoToken != null && currentKeyPairAlias != null) {
            try {
                final PublicKey currentPublicKey = cryptoTokenManagementSession.getPublicKey(authenticationToken, currentCryptoToken.intValue(), currentKeyPairAlias);
                for (final String signatureAlgorithm : AlgorithmTools.getSignatureAlgorithms(currentPublicKey)) {
                    availableSignatureAlgorithms.add(new SelectItem(signatureAlgorithm));
                }
                if (currentSignatureAlgorithm == null && !availableSignatureAlgorithms.isEmpty()) {
                    currentSignatureAlgorithm = (String) availableSignatureAlgorithms.get(0).getValue();
                }
            } catch (Exception e) {
                // No longer active (CryptoTokenOfflineException) or No longer authorized (AuthorizationDeniedException)
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
                currentCryptoToken = null;
                currentKeyPairAlias = null;
            }
        }
        return availableSignatureAlgorithms;
    }
    
    private Integer currentCertificateAuthority = null;
    private String currentCertificateSerialNumber = null;
    private ListDataModel/*<SimpleEntry<Integer, BigInteger>>*/ trustedCertificates = null;
    public Integer getCurrentCertificateAuthority() { return currentCertificateAuthority; }
    public void setCurrentCertificateAuthority(Integer currentCertificateAuthority) { this.currentCertificateAuthority = currentCertificateAuthority; }

    public List<SelectItem/*<Integer,String>*/> getAvailableCertificateAuthorities() {
        final List<Integer> availableCaIds = caSession.getAvailableCAs();
        final Map<Integer, String> caIdToNameMap = caSession.getCAIdToNameMap();
        final List<SelectItem> availableCertificateAuthorities = new ArrayList<SelectItem>(availableCaIds.size());
        for (final Integer availableCaId : availableCaIds) {
            availableCertificateAuthorities.add(new SelectItem(availableCaId, caIdToNameMap.get(availableCaId)));
        }
        if (currentCertificateAuthority==null && !availableCertificateAuthorities.isEmpty()) {
            currentCertificateAuthority = (Integer) availableCertificateAuthorities.get(0).getValue();
        }
        return availableCertificateAuthorities;
    }
    
    public String getCurrentCertificateSerialNumber() { return currentCertificateSerialNumber; }
    public void setCurrentCertificateSerialNumber(String currentCertificateSerialNumber) { this.currentCertificateSerialNumber = currentCertificateSerialNumber; }

    @SuppressWarnings("unchecked")
    public String getTrustedCertificatesCaName() {
        return caSession.getCAIdToNameMap().get(((SimpleEntry<Integer, BigInteger>)trustedCertificates.getRowData()).getKey());
    }
    @SuppressWarnings("unchecked")
    public String getTrustedCertificatesSerialNumberHex() {
        return ((SimpleEntry<Integer, BigInteger>)trustedCertificates.getRowData()).getValue().toString(16);
    }
    
    public ListDataModel getTrustedCertificates() {
        if (trustedCertificates == null) {
            final int internalKeyBindingId = Integer.parseInt(currentInternalKeyBindingId);
            if (internalKeyBindingId == 0) {
                trustedCertificates = new ListDataModel(new ArrayList<SimpleEntry<Integer, BigInteger>>());
            } else {
                try {
                    final InternalKeyBinding internalKeyBinding = internalKeyBindingSession.getInternalKeyBinding(authenticationToken, internalKeyBindingId);
                    trustedCertificates = new ListDataModel(internalKeyBinding.getTrustedCertificateReferences());
                } catch (AuthorizationDeniedException e) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
                }
            }
        }
        return trustedCertificates;
    }
    
    public void addTrust() {
        final List<SimpleEntry<Integer, BigInteger>> trustedCertificateReferences = (List<SimpleEntry<Integer, BigInteger>>) getTrustedCertificates().getWrappedData();
        final String currentCertificateSerialNumber = getCurrentCertificateSerialNumber();
        if (currentCertificateSerialNumber == null || currentCertificateSerialNumber.trim().length() == 0) {
            trustedCertificateReferences.add(new SimpleEntry<Integer,BigInteger>(getCurrentCertificateAuthority(), null));
        } else {
            trustedCertificateReferences.add(new SimpleEntry<Integer,BigInteger>(getCurrentCertificateAuthority(), new BigInteger(getCurrentCertificateSerialNumber(), 16)));
        }
        trustedCertificates.setWrappedData(trustedCertificateReferences);
    }

    public void removeTrust() {
        final SimpleEntry<Integer, BigInteger> trustEntry = ((SimpleEntry<Integer, BigInteger>)trustedCertificates.getRowData());
        final List<SimpleEntry<Integer, BigInteger>> trustedCertificateReferences = (List<SimpleEntry<Integer, BigInteger>>) getTrustedCertificates().getWrappedData();
        trustedCertificateReferences.remove(trustEntry);
        trustedCertificates.setWrappedData(trustedCertificateReferences);
    }
    
    public ListDataModel/*<InternalKeyBindingProperty>*/ getInternalKeyBindingPropertyList() {
        if (internalKeyBindingPropertyList == null) {
            internalKeyBindingPropertyList = new ListDataModel(internalKeyBindingSession.getAvailableTypesAndProperties(authenticationToken).get(getSelectedInternalKeyBindingType()));
        }
        return internalKeyBindingPropertyList;
    }

    /** @return the current multi-valued property's possible values as JSF friendly SelectItems. */
    @SuppressWarnings("unchecked")
    public List<SelectItem/*<String,String>*/> getPropertyPossibleValues() {
        final List<SelectItem> propertyPossibleValues = new ArrayList<SelectItem>();
        if (internalKeyBindingPropertyList != null) {
            final InternalKeyBindingProperty<? extends Serializable> property = (InternalKeyBindingProperty<? extends Serializable>) internalKeyBindingPropertyList.getRowData();
            for (final Serializable possibleValue : property.getPossibleValues()) {
                propertyPossibleValues.add(new SelectItem(property.getAsEncodedValue(property.getType().cast(possibleValue)), possibleValue.toString()));
            }
        }
        return propertyPossibleValues;
    }

    @SuppressWarnings("unchecked")
    public void createNew() {
        try {
            final Map<Object, Object> dataMap = new HashMap<Object, Object>();
            final List<InternalKeyBindingProperty<? extends Serializable>> internalKeyBindingProperties = 
                    (List<InternalKeyBindingProperty<? extends Serializable>>) internalKeyBindingPropertyList.getWrappedData();
            for (final InternalKeyBindingProperty<? extends Serializable> property : internalKeyBindingProperties) {
                dataMap.put(property.getName(), property.getValue());
            }
            currentInternalKeyBindingId = String.valueOf(internalKeyBindingSession.createInternalKeyBinding(authenticationToken, 
                    selectedInternalKeyBindingType, getCurrentName(), InternalKeyBindingStatus.DISABLED, null, currentCryptoToken.intValue(),
                    currentKeyPairAlias, null));
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(getCurrentName() + " created with id " + currentInternalKeyBindingId));
            inEditMode = false;
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (InternalKeyBindingNameInUseException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (CryptoTokenOfflineException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    @SuppressWarnings("unchecked")
    public void saveCurrent() {
        try {
            final InternalKeyBinding internalKeyBinding = internalKeyBindingSession.getInternalKeyBinding(authenticationToken, Integer.parseInt(currentInternalKeyBindingId));
            internalKeyBinding.setName(getCurrentName());
            if (isCryptoTokenActive()) {
                internalKeyBinding.setCryptoTokenId(currentCryptoToken.intValue());
                internalKeyBinding.setKeyPairAlias(currentKeyPairAlias);
                internalKeyBinding.setSignatureAlgorithm(currentSignatureAlgorithm);
                if (currentNextKeyPairAlias == null || currentNextKeyPairAlias.length() == 0) {
                    internalKeyBinding.setNextKeyPairAlias(null);
                } else {
                    internalKeyBinding.setNextKeyPairAlias(currentNextKeyPairAlias);
                }
            }
            internalKeyBinding.setTrustedCertificateReferences((List<SimpleEntry<Integer, BigInteger>>) trustedCertificates.getWrappedData());
            final List<InternalKeyBindingProperty<? extends Serializable>> internalKeyBindingProperties = 
                    (List<InternalKeyBindingProperty<? extends Serializable>>) internalKeyBindingPropertyList.getWrappedData();
            for (final InternalKeyBindingProperty<? extends Serializable> property : internalKeyBindingProperties) {
                internalKeyBinding.setProperty(property.getName(), property.getValue());
            }
            currentInternalKeyBindingId = String.valueOf(internalKeyBindingSession.persistInternalKeyBinding(authenticationToken, internalKeyBinding));
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(getCurrentName() + " saved"));
        } catch (AuthorizationDeniedException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (InternalKeyBindingNameInUseException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }
    
    @Deprecated // Remove before final commit
    private void notYetImplementedMessage() {
        final String msg = "Method '" + Thread.currentThread().getStackTrace()[2].getMethodName() + "()' has not yet been implemented.";
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}

