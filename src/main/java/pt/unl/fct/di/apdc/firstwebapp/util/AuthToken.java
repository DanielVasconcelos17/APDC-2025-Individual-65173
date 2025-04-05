package pt.unl.fct.di.apdc.firstwebapp.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2;
	
	public String username;
	public String tokenID;
	public long creationData;
	public long expirationData;
	public String role;
	public String checker;

	public AuthToken() {}
	
	public AuthToken(String username, String role) {
		this.username = username;
		this.tokenID = UUID.randomUUID().toString();
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + EXPIRATION_TIME;
		this.role = role;
		this.checker = createChecker();
	}

	private String createChecker(){
		return DigestUtils.sha512Hex(username+creationData+expirationData);
	}

	public boolean isValid() {
		String expectedChecker = createChecker();
		return System.currentTimeMillis() <= expirationData
				&& checker.equals(expectedChecker);
	}
	
}
