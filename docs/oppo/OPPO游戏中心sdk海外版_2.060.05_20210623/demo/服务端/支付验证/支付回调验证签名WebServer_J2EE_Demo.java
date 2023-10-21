package com.nearme.gcmanagement.manage.callback;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

public class CallBackServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmreYIkPwVovKR8rLHWlFVw7YDfm9uQOJKL89Smt6ypXGVdrAKKl0wNYc3/jecAoPi2ylChfa2iRu5gunJyNmpWZzlCNRIau55fxGW0XEu553IiprOZcaw5OuYGlf60ga8QT6qToP0/dpiL/ZbmNUO9kUhosIjEu22uFgR+5cYyQIDAQAB";
	private static final String RESULT_STR = "result=%s&resultMsg=%s";
	
	private static final String CALLBACK_OK = "OK";
	private static final String CALLBACK_FAIL = "FAIL";
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		NotifyRequestEntity e = new NotifyRequestEntity();
		e.setNotifyId(req.getParameter("notifyId"));
		e.setPartnerOrder(req.getParameter("partnerOrder"));
		e.setProductName(req.getParameter("productName"));
		e.setProductDesc(req.getParameter("productDesc"));
		e.setPrice(Integer.parseInt(req.getParameter("price")));
		e.setCount(Integer.parseInt(req.getParameter("count")));
		e.setAttach(req.getParameter("attach"));
		e.setSign(req.getParameter("sign"));
		String baseString = getBaseString(e);
		boolean check = false;
		try{
			check = doCheck(baseString, e.getSign(), PUBLIC_KEY);
		}catch(Exception ex){
//			logger.error("验签失败baseString=" + baseString + ", sing=" + e.getSign(), ex);
		}
		String result = CALLBACK_OK;
		String resultMsg = "回调成功";
		if(!check){
			result = CALLBACK_FAIL;
			resultMsg = "验签失败";
		}
		resp.getWriter().write(String.format(RESULT_STR, result, URLEncoder.encode(resultMsg, "UTF-8")));
	}

	private String getBaseString(NotifyRequestEntity ne) {
		StringBuilder sb = new StringBuilder();
		sb.append("notifyId=").append(ne.getNotifyId());
		sb.append("&partnerOrder=").append(ne.getPartnerOrder());
		sb.append("&productName=").append(ne.getProductName());
		sb.append("&productDesc=").append(ne.getProductDesc());
		sb.append("&price=").append(ne.getPrice());
		sb.append("&count=").append(ne.getCount());
		sb.append("&attach=").append(ne.getAttach());
		return sb.toString();
	}
	
	public boolean doCheck(String content, String sign, String publicKey) throws Exception {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		byte[] encodedKey = Base64.decodeBase64(publicKey);
		PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

		java.security.Signature signature = java.security.Signature.getInstance("SHA1WithRSA");

		signature.initVerify(pubKey);
		signature.update(content.getBytes("UTF-8"));
		boolean bverify = signature.verify(Base64.decodeBase64(sign));
		return bverify;
	}
	
}

class NotifyRequestEntity {
	private String notifyId;
	private String partnerOrder;
	private String productName;
	private String productDesc;
	private int price;
	private int count;
	private String attach;
	private String sign;
	
	public String getNotifyId() {
		return notifyId == null ? "" : notifyId;
	}
	public void setNotifyId(String notifyId) {
		this.notifyId = notifyId;
	}
	public String getPartnerOrder() {
		return partnerOrder == null ? "" : partnerOrder;
	}
	public void setPartnerOrder(String partnerOrder) {
		this.partnerOrder = partnerOrder;
	}
	public String getProductName() {
		return productName == null ? "" : productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getProductDesc() {
		return productDesc == null ? "" : productDesc;
	}
	public void setProductDesc(String productDesc) {
		this.productDesc = productDesc;
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getAttach() {
		return attach == null ? "" : attach;
	}
	public void setAttach(String attach) {
		this.attach = attach;
	}
	public String getSign() {
		return sign == null ? "" : sign;
	}
	public void setSign(String sign) {
		this.sign = sign;
	}
}