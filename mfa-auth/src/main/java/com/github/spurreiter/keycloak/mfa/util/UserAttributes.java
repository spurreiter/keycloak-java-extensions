package com.github.spurreiter.keycloak.mfa.util;

import org.keycloak.models.UserModel;

public class UserAttributes {
  public final static String PHONE_NUMBER = "phoneNumber";
  public final static String PHONE_NUMBER_VERIFIED = "phoneNumberVerified";

  public final static boolean getBoolean(UserModel user, String attribute) {
    return user.getAttributeStream(attribute).findFirst().map(x -> x == "true").orElse(false);
  }

  public final static String getString(UserModel user, String attribute) {
    return user.getAttributeStream(attribute).findFirst().map(Object::toString).orElse(null);
  }

  public final static boolean hasPhone (UserModel user) {
    return getString(user, PHONE_NUMBER) != null;
  }

  public final static boolean isPhoneVerified(UserModel user) {
    return getBoolean(user, PHONE_NUMBER_VERIFIED);
  }

  public final static boolean isPhoneVerifiedOrNull(UserModel user) {
    boolean hasPhone = hasPhone(user);
    boolean isPhoneVerified = isPhoneVerified(user);
    return !hasPhone || isPhoneVerified;
  }
}
