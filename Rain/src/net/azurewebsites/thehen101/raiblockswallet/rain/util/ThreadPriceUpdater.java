package net.azurewebsites.thehen101.raiblockswallet.rain.util;

import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;

public class ThreadPriceUpdater extends Thread {
	private final Rain rain;
	private final int updateIntervalMS;
	private boolean alive;
	private String price;
	
	public ThreadPriceUpdater(Rain rain, int updateIntervalMS) {
		this.rain = rain;
		this.updateIntervalMS = updateIntervalMS;
		this.price = this.updatePrice();
		this.alive = true;
	}
	
	@Override
	public void run() {
		while (this.alive) {
			try {
				Thread.sleep(this.updateIntervalMS);
				this.price = this.updatePrice();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getPrice() {
		return this.price;
	}
	
	private String updatePrice() {
		String[] price = new String[1];
		price[0] = null;
		String body = "{ \"command\": \"price\" }";
		RequestWithHeader request = new RequestWithHeader(false, body);
		ListenerServerResponse listener = new ListenerServerResponse() {
			@Override
			public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
				if (!Arrays.equals(request.getRequestBytes(), initialRequest.getRequestBytes()))
					return;
				
				String json = new String(receivedRequest.getRequestBytes()).trim();
				
				JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
				JsonPrimitive p = jsonObject.getAsJsonPrimitive("price");
				
				price[0] = p.getAsString();
			}
		};
		rain.getServerManager().addListenerToAll(listener);
		rain.getServerManager().addToConnectedServerQueue(request);
		while (price[0] == null) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		rain.getServerManager().removeListenerFromAll(listener);
		return price[0];
	}
}
