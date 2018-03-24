package com.jaggie.diffgenerator;

import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import net.dongliu.apk.parser.bean.UseFeature;

public class Main {

	public static void main(String args[]) {

		// use third party to extract the apk package name
		String apkFilePath = args[0];

		// extra the package name for the apk files
		String packageName = "";
		try (ApkFile apkFile = new ApkFile(new File(apkFilePath))) {
			ApkMeta apkMeta = apkFile.getApkMeta();
			packageName = apkMeta.getPackageName();
			// System.out.println(apkMeta.getLabel());
			// System.out.println(apkMeta.getPackageName());
			// System.out.println(apkMeta.getVersionCode());
			// for (UseFeature feature : apkMeta.getUsesFeatures()) {
			// System.out.println(feature.getName());
			// }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String fileSeperator = File.separator;
		String rootDirName = packageName + "_patches";
		;
		String rootFilePath = "." + fileSeperator + rootDirName;
		String infoFileName = packageName + ".info";
		String patchFileName = "patch.patch";
		String apkFileName = packageName + ".apk";
		String apkOldFileName= packageName+"_old"+".apk";
		File patchDir = new File(rootFilePath);
		File latestApkFile = new File(rootFilePath + fileSeperator + apkFileName);
		String pendingAddItemString = "";
		List<String> pendingRemoveList = new ArrayList<>();
		List<String> list;

		// make the root dir if not exists
		if (!patchDir.exists()) {
			patchDir.mkdir();
		}

		if (latestApkFile.exists()) {
			// md5 the latest old apk file
			pendingAddItemString = HashUtils.getMd5OfFile(latestApkFile.getAbsolutePath());

			// exists ,add a new old apk folder
			String newItemPatchPath = rootFilePath + fileSeperator + pendingAddItemString;
			File newItemFile = new File(newItemPatchPath);
			newItemFile.mkdir();

			// move the latest old file to patch folder to do the diff later
			if (latestApkFile.renameTo(new File(newItemPatchPath + fileSeperator + apkOldFileName)))
			{
				// copy the apk file to the root folder and be the latest one
//				apkFile.renameTo(new File(rootFilePath + fileSeperator + apkFileName));
				FileUtils.copyTo(apkFilePath, rootFilePath + fileSeperator + apkFileName);
			}
		} else {
			// don't exits,so you are in the initial status ,just
			// copy the apk file to the root folder and be the latest one
			File apkFile = new File(apkFilePath);
//			apkFile.renameTo(new File(rootFilePath + fileSeperator + apkFileName));
			FileUtils.copyTo(apkFilePath, rootFilePath + fileSeperator + apkFileName);

		}

		// read the info file
		File infoFile = new File(rootFilePath + fileSeperator + infoFileName);

		// read it and extract the pending remove item
		list = readRecordFromInfo(infoFile);
		if (list.size() >= Constants.limitPatchNum) {
			for (int i = 0; list.size() - i < Constants.limitPatchNum; i++) {
				pendingRemoveList.add(list.get(i));
				list.remove(0);
			}
		}

		// add the new item into pending diff item list
		if (pendingAddItemString != null && !"".equals(pendingAddItemString.trim())) {
			list.add(pendingAddItemString);
		}

		// remove the pending remove items for performance saving
		for (String pendingRemoveMD5Folder : pendingRemoveList) {
			File file = new File(rootFilePath + fileSeperator + pendingRemoveMD5Folder);
			FileUtils.deleteFiles(file);
		}

		// TODO
		// do the differences here
		for (String md5 : list) {
			String folder = rootFilePath + fileSeperator + md5;
			String newPatchFilePath = rootFilePath + fileSeperator + md5 + fileSeperator + patchFileName;
			String oldApkFilePath = rootFilePath + fileSeperator + md5 + fileSeperator + apkOldFileName;
			String newApkFilePath = rootFilePath + fileSeperator + apkFileName;
			// do the single difference here
			doTheDifference(oldApkFilePath, newApkFilePath, newPatchFilePath);
		}

		// write back the current status into xxx.info file
		logDownRecordIntoInfo(infoFile, list);

	}

	private static List<String> readRecordFromInfo(File infoFile) {
		try {
			List<String> infos = new ArrayList<>();

			if (infoFile.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(infoFile));
				for (String line; (line = br.readLine()) != null;) {
					// process the line.
					infos.add(line.trim());
				}
			}
			return infos;
			// line is not visible here.
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		// return null;
	}

	private static void logDownRecordIntoInfo(File infoFile, List<String> lists) {

		if (infoFile.exists()) {
			infoFile.delete();
		}
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(infoFile.getAbsolutePath(), true)));

			for (String md5 : lists) {
				out.println(md5);
			}
			out.close();
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}
	}

	private static void doTheDifference(String oldApkFilePath, String newApkFilePath, String patchFilePath) {
		String commandResult=executeCommand("."+File.separator+"bsdiff "+oldApkFilePath+" "+newApkFilePath +" "+patchFilePath);
	}

	private static String executeCommand(String command) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();

	}
}
