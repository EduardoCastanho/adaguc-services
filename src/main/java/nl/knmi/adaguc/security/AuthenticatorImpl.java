package nl.knmi.adaguc.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.AuthenticationException;

import nl.knmi.adaguc.security.PemX509Tools.X509Info;
import nl.knmi.adaguc.security.token.Token;
import nl.knmi.adaguc.security.token.TokenManager;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.ElementNotFoundException;
import nl.knmi.adaguc.tools.HTTPTools;

public class AuthenticatorImpl implements AuthenticatorInterface{

	X509Info x509 = null;
	public AuthenticatorImpl(HttpServletRequest request) {
		init(request);
	}

	public AuthenticatorImpl() {
	}

	@Override
	public synchronized void init(HttpServletRequest request) {
		if (request == null ) {
			return;
		}
		
		try{
			String userName = SecurityConfigurator.getUser();
			if (userName != null) {
				request.getSession().setAttribute("user_identifier",userName);
				request.getSession().setAttribute("services_access_token",userName);
				request.getSession().setAttribute("emailaddress",userName);
			}
		}catch(Exception e){
		}
		
		/* Get user from session */
		String sessionId = null;
		HttpSession session = request.getSession();
		if (session!=null) {
			sessionId = (String) session.getAttribute("user_identifier");
		}
		if (sessionId!=null) {
			x509 = new PemX509Tools().new X509Info(sessionId, sessionId);
			return;
		}

		/* Get user from header (Set by SSL client cert verification in NGINX)*/
		try {
//			Enumeration headerNames = request.getHeaderNames();
//			while(headerNames.hasMoreElements()) {
//			  String headerName = (String)headerNames.nextElement();
//			  Debug.println("" + headerName);
//			  Debug.println("" + request.getHeader(headerName));
//			}
			String userHeader = SecurityConfigurator.getUserHeader();
			if (userHeader != null) {
				String userIdFromHeader = request.getHeader(userHeader);
				if (userIdFromHeader != null && userIdFromHeader.length() > 4) {
					String userID = new PemX509Tools().getUserIdFromSubjectDN(userIdFromHeader);
					Debug.println("Found user from header: " + userID);
					x509 = new PemX509Tools().new X509Info(userID, userID);
					return;
				}
			}
		} catch (ElementNotFoundException e) {
		} catch (IOException e) {
		}

		x509 = new PemX509Tools().getUserIdFromCertificate(request);
		Debug.println("No user info found from certificates");
		if(x509 == null){
			String path = request.getServletPath();

			String tokenStr = new TokenManager().getTokenFromPath(path);

			if(tokenStr == null){
				try {
					tokenStr = HTTPTools.getHTTPParam(request, "key");
				} catch (Exception e1) {
					Debug.println("No access token set in URL via key=<accesstoken> KVP");
				}
			}

			if(tokenStr!=null){
				Token token = null;
				try {
					token = TokenManager.getToken(tokenStr);
					x509 = new PemX509Tools().new X509Info(token.getUserId(), token.getToken());
				} catch (AuthenticationException | IOException | ElementNotFoundException e1) {
					Debug.printStackTrace(e1);
				}



			}else{
				Debug.println("Unable to find user info from certificate or accesstoken");
			}



		}

	}

	public String getClientId(){
		if(x509 == null){
			return null;
		}
		return x509.getCN();
	}


}
