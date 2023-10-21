var crypto = require('crypto');
var URL = require('url');
var urlencode = require('urlencode');
var querystring = require('querystring');
var http = require('http');
var https = require('https');
var url = require('url');
var util = require('util');
var querystring = require('querystring');

// 一些乱七八糟的东西
function md5(input) {
    return crypto.createHash('md5').update(input).digest('hex');
}

// hamcsha1
function hmac(input, key, out) {
    return out ? crypto.createHmac('sha1', key).update(input).digest(out) : crypto.createHmac('sha1', key).update(input).digest('hex');
}
 ///
exports.HttpGet_HeaderJson = function(url_str, data, callback) {
    if ('string' === typeof url_str)
        var  parse_u = url.parse(url_str);
    var isHttp = (parse_u.protocol == 'http:');
    var options = {
        host: parse_u.hostname,
        port: parse_u.port || (isHttp ? 80 : 443),
        path: parse_u.path,
        method: 'get',
        headers: {
            'param':data.param,
            'oauthSignature':data.oauthSignature
        }
    };
    (isHttp ? http : https).get(options, function(response) {
        var res_data = '';
        response.on('data', function(chunk){
            res_data += chunk;
        });
        response.on('end', function(){
            if (callback) callback(undefined, res_data);
        });
    }).on('error', function(e) {
            callback(e.message);
        });
}
////
 exports.login=function(params, callback) {

        var access_tokens = JSON.parse( params.session);
        var access_token = access_tokens.token;  //token
        var access_token_secret = access_tokens.ssoid;   //ssoid
        if(!access_token)
        {
            console.LOG("---oppo-params.access_token 未定义或者传过来为空！！！");
        }
        if(!access_token_secret)
        {
            console.LOG("---oppo-params.access_token_secret 未定义或者传过来为空！！！");
        }
        console.LOG("---oppo-access_token="+access_token);
        console.LOG("---oppo-access_token_secret="+access_token_secret);

        var appkey = "5x1lsOHxxfGgc4cgoggK8sSkg";
        var appSecret = "dc155c455844170a30B60Ba60be35b83";
        var url="https://in-gameopen.oppomobile.com/sdkopen/user/fileIdInfo";
        var request_serverUrl = url+"?fileId="+urlencode.encode(access_token_secret ,'utf8')+"&token="+ urlencode.encode(access_token,'utf8');
        var nonce = parseInt( Math.random() * ((9999999999-1000000000)+1000000000));
        var ts = parseInt((new Date()).getTime()/1000);
        console.LOG("---oppo-request_serverUrl="+request_serverUrl);

        var json = {
            oauthConsumerKey: appkey,
            oauthToken: urlencode.encode(access_token ,'utf8'),
            oauthSignatureMethod: 'HMAC-SHA1',
            oauthTimestamp: ts,
            oauthNonce: nonce,
            oauthVersion: '1.0'
        };
        var baseStr = "";
        for(var key in json){
            baseStr+=key+"="+json[key]+"&";
        }
        console.LOG("---oppo-baseStr="+baseStr);

        var beseString = new Buffer(baseStr,'utf8').toString();
        baseStr = beseString.replace(/\+/g, "%2B");

        var sign = urlencode.encode(hmac(baseStr, appSecret+ "&", 'base64'),'utf8').toString();

        console.LOG("---oppo-sign="+sign);

        var authString ={
            "param":baseStr,
            "oauthSignature":sign
        };
        console.LOG("---authString=%s",JSON.stringify(authString));

         this.HttpGet_HeaderJson(request_serverUrl, authString, function (err, ret) {
            try {
                if (err) {
                    console.LOG("Oppo 帐号get请求异常:" + err);
                    return callback("LoginFail");
                }

                var retData = JSON.parse(ret);
                console.LOG("Oppo 帐号登陆回调信息：" +ret);
                if(retData.resultCode != 200)
                {
                    console.LOG("Oppo 帐号登陆失败:" + retData.resultMsg);
                    return callback("LoginFail");
                }

                if (!retData.ssoid || !retData.ssoid != access_token_secret) {
                    console.LOG("Oppo 帐号登陆失败 access_token_secret=[%s] retData.ssoid=[%s]" ,access_token_secret,retData.ssoid);
                    return callback("LoginFail");
                }

                var user = {userid : retData.ssoid};
                console.LOG("Oppo 帐号登陆成功:" + retData);
                return callback(undefined, user);

            } catch (err) {
                console.LOG("Oppo 登陆异常" + err);
                return callback("LoginFail");
            }
        });
}
