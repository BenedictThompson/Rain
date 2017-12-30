package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

public final class FileUtil {
	
	/**
	 * Converts a File into a multi-line String using a BufferedReader.
	 * 
	 * @param file The file to be read.
	 * @return The contents of the file as a multi-line String.
	 */
	public static final String fileToString(File file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String readLine, lines = "";
			while ((readLine = br.readLine()) != null) {
				lines += readLine + System.getProperty("line.separator");
			}
			br.close();
			return lines;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return ioe.getMessage();
		}
	}
	
	/**
	 * Writes a multi-line String into a file.
	 * @param file The file to be written to.
	 * @param s The string to be written to the file.
	 * @return The contents of the file as a multi-line String.
	 */
	public static final void stringToFile(File file, String s) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
			Scanner scanner = new Scanner(s);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				bw.write(line + System.getProperty("line.separator"));
			}
			scanner.close();
			bw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static final void bytesToFile(File file, byte[] bytes) {
		try {
			Files.write(file.toPath(), bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static final byte[] fileToBytes(File file) {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
