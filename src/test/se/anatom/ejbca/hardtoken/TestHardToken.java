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

package se.anatom.ejbca.hardtoken;

import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.hardtoken.IHardTokenSessionHome;
import org.ejbca.core.ejb.hardtoken.IHardTokenSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.hardtoken.HardTokenData;
import org.ejbca.core.model.hardtoken.types.SwedishEIDHardToken;
import org.ejbca.core.model.hardtoken.types.TurkishEIDHardToken;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

/**
 * Tests the hard token related entity beans.
 *
 * @version $Id: TestHardToken.java,v 1.7 2007-04-13 06:23:55 herrvendil Exp $
 */
public class TestHardToken extends TestCase {
    private static Logger log = Logger.getLogger(TestHardToken.class);
    private IHardTokenSessionRemote cacheAdmin;
    private ICertificateStoreSessionRemote certStore;
    private IRaAdminSessionRemote raAdmin;


    private static IHardTokenSessionHome cacheHome;
    private static ICertificateStoreSessionHome storeHome;
    private static IRaAdminSessionHome raAdminHome;
    
    private static int orgEncryptCAId;

    private static final Admin admin = new Admin(Admin.TYPE_INTERNALUSER);

    static byte[] testcert = Base64.decode(("MIICWzCCAcSgAwIBAgIIJND6Haa3NoAwDQYJKoZIhvcNAQEFBQAwLzEPMA0GA1UE"
            + "AxMGVGVzdENBMQ8wDQYDVQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMB4XDTAyMDEw"
            + "ODA5MTE1MloXDTA0MDEwODA5MjE1MlowLzEPMA0GA1UEAxMGMjUxMzQ3MQ8wDQYD"
            + "VQQKEwZBbmFUb20xCzAJBgNVBAYTAlNFMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCB"
            + "hwKBgQCQ3UA+nIHECJ79S5VwI8WFLJbAByAnn1k/JEX2/a0nsc2/K3GYzHFItPjy"
            + "Bv5zUccPLbRmkdMlCD1rOcgcR9mmmjMQrbWbWp+iRg0WyCktWb/wUS8uNNuGQYQe"
            + "ACl11SAHFX+u9JUUfSppg7SpqFhSgMlvyU/FiGLVEHDchJEdGQIBEaOBgTB/MA8G"
            + "A1UdEwEB/wQFMAMBAQAwDwYDVR0PAQH/BAUDAwegADAdBgNVHQ4EFgQUyxKILxFM"
            + "MNujjNnbeFpnPgB76UYwHwYDVR0jBBgwFoAUy5k/bKQ6TtpTWhsPWFzafOFgLmsw"
            + "GwYDVR0RBBQwEoEQMjUxMzQ3QGFuYXRvbS5zZTANBgkqhkiG9w0BAQUFAAOBgQAS"
            + "5wSOJhoVJSaEGHMPw6t3e+CbnEL9Yh5GlgxVAJCmIqhoScTMiov3QpDRHOZlZ15c"
            + "UlqugRBtORuA9xnLkrdxYNCHmX6aJTfjdIW61+o/ovP0yz6ulBkqcKzopAZLirX+"
            + "XSWf2uI9miNtxYMVnbQ1KPdEAt7Za3OQR6zcS0lGKg==").getBytes());


    /**
     * Creates a new TestHardToken object.
     *
     * @param name name
     */
    public TestHardToken(String name) {
        super(name);
    }

    protected void setUp() throws Exception {

        log.debug(">setUp()");
        CertTools.installBCProvider();
        if (cacheAdmin == null) {
            if (cacheHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(IHardTokenSessionHome.JNDI_NAME);
                cacheHome = (IHardTokenSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IHardTokenSessionHome.class);

            }

            cacheAdmin = cacheHome.create();
        }
        if (certStore == null) {
            if (storeHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(ICertificateStoreSessionHome.JNDI_NAME);
                storeHome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, ICertificateStoreSessionHome.class);
            }

            certStore = storeHome.create();
        } 
        
        if (raAdmin == null) {
            if (raAdminHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(IRaAdminSessionHome.JNDI_NAME);
                raAdminHome = (IRaAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IRaAdminSessionHome.class);
            }

            raAdmin = raAdminHome.create();
        } 


        log.debug("<setUp()");
    }

    protected void tearDown() throws Exception {
    }

    private Context getInitialContext() throws NamingException {
        log.debug(">getInitialContext");

        Context ctx = new javax.naming.InitialContext();
        log.debug("<getInitialContext");

        return ctx;
    }


    /**
     * adds a token to the database
     *
     * @throws Exception error
     */

    public void test01AddHardToken() throws Exception {
        log.debug(">test01AddHardToken()");
      
        GlobalConfiguration gc = raAdmin.loadGlobalConfiguration(admin);
        orgEncryptCAId = gc.getHardTokenEncryptCA();
        gc.setHardTokenEncryptCA(0);
        raAdmin.saveGlobalConfiguration(admin, gc);
        

        SwedishEIDHardToken token = new SwedishEIDHardToken("1234", "1234", "123456", "123456", 1);

        ArrayList certs = new ArrayList();

        certs.add(CertTools.getCertfromByteArray(testcert));

        cacheAdmin.addHardToken(admin, "1234", "TESTUSER", "CN=TEST", SecConst.TOKEN_SWEDISHEID, token, certs, null);

        TurkishEIDHardToken token2 = new TurkishEIDHardToken("1234",  "123456", 1);

        cacheAdmin.addHardToken(admin, "2345", "TESTUSER", "CN=TEST", SecConst.TOKEN_TURKISHEID, token2, certs, null);

        

        log.debug("<test01AddHardToken()");
    }


    /**
     * edits token
     *
     * @throws Exception error
     */
    
    public void test02EditHardToken() throws Exception {
        log.debug(">test02EditHardToken()");

        boolean ret = false;

        HardTokenData token = cacheAdmin.getHardToken(admin, "1234", true);

        SwedishEIDHardToken swe = (SwedishEIDHardToken) token.getHardToken();

        assertTrue("Retrieving HardToken failed", swe.getInitialAuthEncPIN().equals("1234"));

        swe.setInitialAuthEncPIN("5678");

        cacheAdmin.changeHardToken(admin, "1234", SecConst.TOKEN_SWEDISHEID, token.getHardToken());
        ret = true;

        assertTrue("Editing HardToken failed", ret);


        log.debug("<test02EditHardToken()");
    }  
    


    /**
     * Test that tries to find a hardtokensn from is certificate
     *
     * @throws Exception error
     */
    
    public void test03FindHardTokenByCertificate() throws Exception {
        log.debug(">test03FindHardTokenByCertificate()");

        X509Certificate cert = CertTools.getCertfromByteArray(testcert);
        // Store the dummy cert for test.  
        if(certStore.findCertificateByFingerprint(admin, CertTools.getFingerprintAsString(cert)) == null){
          certStore.storeCertificate(admin,cert,"DUMMYUSER", CertTools.getFingerprintAsString(cert),CertificateDataBean.CERT_ACTIVE,CertificateDataBean.CERTTYPE_ENDENTITY);
        }
        String tokensn = cacheAdmin.findHardTokenByCertificateSNIssuerDN(admin, cert.getSerialNumber(), cert.getIssuerDN().toString());        

        assertTrue("Couldn't find right hardtokensn", tokensn.equals("1234"));

        log.debug("<test03FindHardTokenByCertificate()");
    }
    
    /**
     * edits token
     *
     * @throws Exception error
     */
    
    public void test04EncryptHardToken() throws Exception {
        log.debug(">test04EncryptHardToken()");

        GlobalConfiguration gc = raAdmin.loadGlobalConfiguration(admin);
        gc.setHardTokenEncryptCA("CN=TEST".hashCode());
        raAdmin.saveGlobalConfiguration(admin, gc);
        boolean ret = false;

        // Make sure the old data can be read
        HardTokenData token = cacheAdmin.getHardToken(admin, "1234", true);

        SwedishEIDHardToken swe = (SwedishEIDHardToken) token.getHardToken();

        assertTrue("Retrieving HardToken failed : " + swe.getInitialAuthEncPIN(), swe.getInitialAuthEncPIN().equals("5678"));

        swe.setInitialAuthEncPIN("5678");

        // Store the new data as encrypted
        cacheAdmin.changeHardToken(admin, "1234", SecConst.TOKEN_SWEDISHEID, token.getHardToken());
        ret = true;                

        assertTrue("Saving encrypted HardToken failed", ret);

        // Make sure the encrypted data can be read
        token = cacheAdmin.getHardToken(admin, "1234",true);

        swe = (SwedishEIDHardToken) token.getHardToken();

        assertTrue("Retrieving encrypted HardToken failed", swe.getInitialAuthEncPIN().equals("5678"));

        log.debug("<test04EncryptHardToken()");
    }
    
    /**
     * removes all profiles
     *
     * @throws Exception error
     */
   
    public void test05removeHardTokens() throws Exception {
        log.debug(">test05removeHardTokens()");
        GlobalConfiguration gc = raAdmin.loadGlobalConfiguration(admin);
        gc.setHardTokenEncryptCA(orgEncryptCAId);
        raAdmin.saveGlobalConfiguration(admin, gc);
        boolean ret = false;
        try {
            cacheAdmin.removeHardToken(admin, "1234");
            cacheAdmin.removeHardToken(admin, "2345");

            ret = true;
        } catch (Exception pee) {
        }
        assertTrue("Removing Hard Token failed", ret);

        log.debug("<test05removeHardTokens()");
    }
   

}
