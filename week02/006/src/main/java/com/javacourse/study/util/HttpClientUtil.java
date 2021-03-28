package com.javacourse.study.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpClientUtil {
	private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
	public static final PoolingHttpClientConnectionManager CONN_MANAGER;
	public static final CloseableHttpClient HTTP_CLIENT;

	static {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(new TrustStrategy() {

				@Override
				public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

					return true;
				}
			}).build();
		} catch (Exception e) {
			throw new RuntimeException("failed to get SSLContext", e);
		}

		HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build();
		CONN_MANAGER = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		CONN_MANAGER.setDefaultMaxPerRoute(50);
		CONN_MANAGER.setMaxTotal(100);
		HTTP_CLIENT = HttpClients.custom().setConnectionManager(CONN_MANAGER)
				.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
				.setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
				.setRetryHandler(new DefaultHttpRequestRetryHandler()).build();

	}

	public static Result sendGet(String url, String encoding, Map<String, ? extends Object> params, int timeout)
			throws Exception {
		return sendGet(url, encoding, params, null, timeout);
	}

	public static Result sendGet(String url, String encoding, Map<String, ? extends Object> params,
			Map<String, String> headers, int timeout) throws Exception {

		StringBuilder sb = new StringBuilder();

		boolean isfirst = true;
		if (params != null && !params.isEmpty()) {
			Set<String> ks = params.keySet();
			for (String s : ks) {
				Object v = params.get(s);
				if (isfirst) {
					isfirst = false;
					if (!url.contains("?")) {
						sb.append("?").append(s).append("=").append(URLEncoder.encode(v.toString(), encoding));
					} else {
						sb.append("&").append(s).append("=").append(URLEncoder.encode(v.toString(), encoding));
					}
				} else {
					sb.append("&").append(s).append("=").append(URLEncoder.encode(v.toString(), encoding));
				}
			}
		}

		url = url + sb.toString();
		HttpGet httpget = new HttpGet(url);// HTTP Get请求

		// 设置请求和传输超时时间
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
				.setConnectionRequestTimeout(timeout).build();
		httpget.setConfig(requestConfig);

		if (headers != null) {
			Set<Entry<String, String>> set = headers.entrySet();
			for (Entry<String, String> entry : set) {
				httpget.addHeader(entry.getKey(), entry.getValue());
			}
		}

		HttpEntity entity = null;
		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = HTTP_CLIENT.execute(httpget);
			StatusLine st = httpResponse.getStatusLine();
			entity = httpResponse.getEntity();
			String content = EntityUtils.toString(entity, encoding);

			return new Result(st.getStatusCode(), content);
		} finally {
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
			if (httpResponse != null) {
				try {
					httpResponse.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	public static Result sendPost(String url, String encoding, Map<String, ? extends Object> params, int timeout)
			throws Exception {
		return sendPost(url, encoding, params, null, timeout);
	}

	public static Result sendPostJSON(String url, String encoding, Map<String, ? extends Object> params,
			Map<String, String> headers, int timeout) throws Exception {
		HttpPost httpPost = new HttpPost(url);

		// 设置请求和传输超时时间
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
				.setConnectionRequestTimeout(timeout).build();
		httpPost.setConfig(requestConfig);

		 //设置参数
        JSONObject jsonObject = new JSONObject();
        if (params != null && !params.isEmpty()) {
			for (Entry<String, ? extends Object> e : params.entrySet()) {
				jsonObject.put(e.getKey(), e.getValue());
			}
		}

		if (headers != null) {
			Set<Entry<String, String>> set = headers.entrySet();
			for (Entry<String, String> entry : set) {
				httpPost.addHeader(entry.getKey(), entry.getValue());
			}
		}

		HttpEntity entity = null;
		CloseableHttpResponse httpResponse = null;
		try {
			String paramJson = jsonObject.toString();
			httpPost.setEntity(new StringEntity(jsonObject.toString(),"UTF-8"));

			httpResponse = HTTP_CLIENT.execute(httpPost);
			StatusLine st = httpResponse.getStatusLine();
			entity = httpResponse.getEntity();
			String content = EntityUtils.toString(entity, encoding);
			return new Result(st.getStatusCode(), content);
		} finally {
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
			if (httpResponse != null) {
				try {
					httpResponse.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}

	}

	public static Result sendPost(String url, String encoding, Map<String, ? extends Object> params,
			Map<String, String> headers, int timeout) throws Exception {
		HttpPost httpPost = new HttpPost(url);

		// 设置请求和传输超时时间
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
				.setConnectionRequestTimeout(timeout).build();
		httpPost.setConfig(requestConfig);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		if (params != null && !params.isEmpty()) {
			for (Entry<String, ? extends Object> e : params.entrySet()) {
				nvps.add(new BasicNameValuePair(e.getKey(), e.getValue().toString()));
			}
		}

		if (headers != null) {
			Set<Entry<String, String>> set = headers.entrySet();
			for (Entry<String, String> entry : set) {
				httpPost.addHeader(entry.getKey(), entry.getValue());
			}
		}

		HttpEntity entity = null;
		CloseableHttpResponse httpResponse = null;
		try {

			httpPost.setEntity(new UrlEncodedFormEntity(nvps, encoding));
			//httpPost.setEntity(new StringEntity(jsonObject.toString()));

			httpResponse = HTTP_CLIENT.execute(httpPost);
			StatusLine st = httpResponse.getStatusLine();
			entity = httpResponse.getEntity();
			String content = EntityUtils.toString(entity, encoding);
			return new Result(st.getStatusCode(), content);
		} finally {
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
			if (httpResponse != null) {
				try {
					httpResponse.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}

	}


	/**
	 * 直接将内容放在post体里面
	 *
	 * @param url
	 * @param encoding
	 * @param content
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static Result sendPostJSON(String url, String encoding, String contentw, int timeout) throws Exception {
		HttpPost httpPost = new HttpPost(url);
		// 设置请求和传输超时时间
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
				.setConnectionRequestTimeout(timeout).build();
		httpPost.setConfig(requestConfig);

		httpPost.addHeader("Content-type", "application/json; charset=utf-8");
		httpPost.setEntity(new StringEntity(contentw, Charset.forName(encoding)));

		HttpEntity entity = null;
		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = HTTP_CLIENT.execute(httpPost);
			StatusLine st = httpResponse.getStatusLine();
			entity = httpResponse.getEntity();
			String resultstring = EntityUtils.toString(entity, encoding);
			return new Result(st.getStatusCode(), resultstring);
		} finally {
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
			if (httpResponse != null) {
				try {
					httpResponse.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	public static class Result {
		private int status;
		private String content;

		public Result(int status, String content) {
			this.status = status;
			this.content = content;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		@Override
		public String toString() {
			return "[" + status + "][" + content + "]";
		}

	}

    public static void main(String[] args) throws Exception {
	    sendGet("http://localhost:8801",null,null,3000)

    }
}
