package net.azurewebsites.thehen101.raiblockswallet.rain.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A utility class to convert denominations of Raiblocks. BigInteger and
 * BigDecimal are used as large numbers with many trailing zeroes are to be
 * processed to convert from one denomination to another.
 * 
 * @author thehen101
 *
 */
public enum DenominationConverter {
	MRAI(BigInteger.ONE), // there is one mrai in one mrai
	KRAI(new BigInteger("1000")), // there are 1000 krai in one mrai
	RAI(new BigInteger("1000000")), // one million rai in one mrai
	RAW(new BigInteger("1000000000000000000000000000000"));
	
	private BigInteger valueComparedToMrai;
	
	DenominationConverter(BigInteger raiWorth) {
		this.valueComparedToMrai = raiWorth;
	}
	
	public static BigDecimal convert(BigDecimal amount, DenominationConverter from, DenominationConverter to) {
		return amount.multiply(new BigDecimal(to.valueComparedToMrai).divide(new BigDecimal(from.valueComparedToMrai)));
	}
	
	public static BigInteger convertToRaw(BigInteger amount, DenominationConverter from) {
		return amount.multiply(RAW.valueComparedToMrai.divide(from.valueComparedToMrai));
	}
}
