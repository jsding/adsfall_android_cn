<?php
define("PUBLIC_KEY",'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmreYIkPwVovKR8rLHWlFVw7YDfm9uQOJKL89Smt6ypXGVdrAKKl0wNYc3/jecAoPi2ylChfa2iRu5gunJyNmpWZzlCNRIau55fxGW0XEu553IiprOZcaw5OuYGlf60ga8QT6qToP0/dpiL/ZbmNUO9kUhosIjEu22uFgR+5cYyQIDAQAB');//跟充值平台通信的加密key

function rsa_verify($contents) {
	$str_contents = "notifyId={$contents['notifyId']}&partnerOrder={$contents['partnerOrder']}&productName={$contents['productName']}&productDesc={$contents['productDesc']}&price={$contents['price']}&count={$contents['count']}&attach={$contents['attach']}";
	$publickey= PUBLIC_KEY;
	$pem = chunk_split($publickey,64,"\n");
	$pem = "-----BEGIN PUBLIC KEY-----\n".$pem."-----END PUBLIC KEY-----\n";
	$public_key_id = openssl_pkey_get_public($pem);
	$signature =base64_decode($contents['sign']);
	return openssl_verify($str_contents, $signature, $public_key_id );//成功返回1,0失败，-1错误,其他看手册
}

function get_param($param_name)//兼容post/get
{
	$param_value = "";
	if(isset($_POST[$param_name])){
		$param_value = trim($_POST[$param_name]);
	}else if(isset($_GET[$param_name])){
		$param_value = trim($_GET[$param_name]);
	}
	return $param_value;
}

$data['notifyId'] = get_param('notifyId');
$data['partnerOrder'] = get_param('partnerOrder');
$data['productName'] = get_param('productName');
$data['productDesc'] = get_param('productDesc');
$data['price'] = get_param('price');
$data['count'] = get_param('count');
$data['attach'] = get_param('attach');
$data['sign'] = get_param('sign');

$result = rsa_verify($data);

echo($result);

?>