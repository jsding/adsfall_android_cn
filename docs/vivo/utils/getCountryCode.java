public static String getCountryCode() {
	
	String countryCode = getSystemProperties("ro.product.country.region", "");
	if (TextUtils.isEmpty(countryCode)) {
		countryCode = getSystemProperties("ro.product.customize.bbk", "");
	}
	return countryCode;
}

public static String getSystemProperties(String key, String def) {
	String value = null;

	try {
		Method method = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
		value = (String) method.invoke(null, key);
	} catch (Exception e) {
		e.printStackTrace();
	}

	if (TextUtils.isEmpty(value)) {
		value = def;
	}

	return value;
}