package com.jaggie.diffgenerator;

import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import net.dongliu.apk.parser.bean.UseFeature;

public class Main {

	public static void main(String args[]) {

		// use third party to extract the apk package name
		String pendingLatestApkFilePath = args[0];

		// extra the package name for the apk files
		String packageName = "";
		try (ApkFile apkFile = new ApkFile(new File(pendingLatestApkFilePath))) {
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
		String rootFilePath = "." + fileSeperator + rootDirName;
		String infoFileName = packageName + ".info";
		String patchFileName = "patch.patch";
		String currentLatestApkFileName = packageName + "_latest.apk";
		String apkOldFileName = packageName + "_old" + ".apk";
		File patchDir = new File(rootFilePath);
		File currentLatestApkFile = null;
		File currentlatestTagFile = null;
		String pendingAddItemString = "";
		List<String> pendingRemoveList = new ArrayList<>();
		List<String> list;

		// make the root dir if not exists
		if (!patchDir.exists()) {
			patchDir.mkdir();
		} else {
			// filter the name of ".tag"
			// String[] fileNames = patchDir.list(new FilenameFilter() {
			// public boolean accept(File dir, String filename) {
			// return filename.endsWith(".tag");
			// }
			// });
			//
			// if (fileNames != null && fileNames.length >= 1) {
			// currentlatestTagFile = new File(rootFilePath + fileSeperator
			// + fileNames[0]);
			// }

			// find the current latest apk file
			currentLatestApkFile = new File(rootFilePath + fileSeperator
					+ currentLatestApkFileName);

			// find the current latest tag file
			String currentlatestTagFileName = HashUtils
					.getMd5OfFile(currentLatestApkFile.getAbsolutePath())
					+ ".tag";
			currentlatestTagFile = new File(rootFilePath + fileSeperator
					+ currentlatestTagFileName);
		}

		if (currentLatestApkFile != null && currentLatestApkFile.exists()) {
			// md5 the latest old apk file
			pendingAddItemString = HashUtils.getMd5OfFile(currentLatestApkFile
					.getAbsolutePath());

			// exists ,add a new old apk folder
			String newItemPatchPath = rootFilePath + fileSeperator
					+ pendingAddItemString;
			File newItemFile = new File(newItemPatchPath);
			newItemFile.mkdir();

			// move the latest old file to patch folder to do the diff later
			boolean cutResult = currentLatestApkFile.renameTo(new File(
					newItemPatchPath + fileSeperator + apkOldFileName));
			// copy the apk file to the root folder and be the latest one
			// apkFile.renameTo(new File(rootFilePath + fileSeperator +
			// apkFileName));
			if (!cutResult) {
				// cut failed,just remove the one and cut again
				FileUtils.deleteFiles(new File(newItemPatchPath + fileSeperator
						+ apkOldFileName));
				cutResult = currentLatestApkFile.renameTo(new File(
						newItemPatchPath + fileSeperator + apkOldFileName));
			}
			FileUtils.copyTo(pendingLatestApkFilePath, rootFilePath
					+ fileSeperator + currentLatestApkFileName);

		} else {
			// don't exists,so you are in the initial status

			// apkFile.renameTo(new File(rootFilePath + fileSeperator +
			// apkFileName));

		}

		// copy the apk file to the root folder and be the latest one
		FileUtils.copyTo(pendingLatestApkFilePath, rootFilePath + fileSeperator
				+ currentLatestApkFileName);

		// remove the tag file to add a new tag file
		if (currentlatestTagFile != null && currentlatestTagFile.exists()) {
			FileUtils.deleteFiles(currentlatestTagFile);
		}

		String pendingLatestTagFileName = HashUtils
				.getMd5OfFile(pendingLatestApkFilePath) + ".tag";
		File pendingLatestTagFile = new File(rootFilePath + fileSeperator
				+ pendingLatestTagFileName);
		FileUtils.createNewFile(pendingLatestTagFile);

		// read the info file
		File infoFile = new File(rootFilePath + fileSeperator + infoFileName);

		// read it and extract the pending remove item
		list = readRecordFromInfo(infoFile);
		for (int i = 0; list.size() > Constants.limitPatchNum; i++) {
			pendingRemoveList.add(list.get(i));
			list.remove(0);
		}

		// add the new item into pending diff item list
		if (pendingAddItemString != null
				&& !"".equals(pendingAddItemString.trim()) && list != null
				&& !list.contains(pendingAddItemString)) {
			list.add(pendingAddItemString);
		}

		// remove the pending remove items for performance saving
		for (String pendingRemoveMD5Folder : pendingRemoveList) {
			File file = new File(rootFilePath + fileSeperator
					+ pendingRemoveMD5Folder);
			FileUtils.deleteFiles(file);
		}

		// TODO
		// do the differences here
		for (String md5 : list) {
			String folder = rootFilePath + fileSeperator + md5;
			String newPatchFilePath = rootFilePath + fileSeperator + md5
					+ fileSeperator + patchFileName;
			String oldApkFilePath = rootFilePath + fileSeperator + md5
					+ fileSeperator + apkOldFileName;
			String newApkFilePath = rootFilePath + fileSeperator
					+ currentLatestApkFileName;
			// do the single difference here
			doTheDifference(oldApkFilePath, newApkFilePath, newPatchFilePath);
		}

		// write back the current status into the xxx.info file
		logDownRecordIntoInfo(infoFile, list);

	}

	private static List<String> readRecordFromInfo(File infoFile) {
		BufferedReader br=null;
		try {
			List<String> infos = new ArrayList<>();

			if (infoFile.exists()) {
				br = new BufferedReader(new FileReader(infoFile));
				for (String line; (line = br.readLine()) != null
						&& !"".equals(line.trim());) {
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
		} finally {

			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// return null;
	}

	private static void logDownRecordIntoInfo(File infoFile, List<String> lists) {

		if (infoFile.exists()) {
			infoFile.delete();
		}
		PrintWriter out=null;
		try {
			out = new PrintWriter(new BufferedWriter(
					new FileWriter(infoFile.getAbsolutePath(), true)));

			for (String md5 : lists) {
				out.println(md5);
			}
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}finally{
			if (out!=null){
				out.close();
			}
		}
	}

	private static void doTheDifference(String oldApkFilePath,
			String newApkFilePath, String patchFilePath) {

		String commandResult;

		String OS = System.getProperty("os.name", "generic").toLowerCase(
				Locale.ENGLISH);
		if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
			commandResult = executeCommand("."+File.separator+"diffcmds"+ File.separator + "bsdiff "
					+ oldApkFilePath + " " + newApkFilePath + " "
					+ patchFilePath);

		} else if (OS.indexOf("win") >= 0) {
			commandResult = executeCommand("."+File.separator+"diffcmds" + File.separator + "bsdiff.exe "
					+ oldApkFilePath + " " + newApkFilePath + " "
					+ patchFilePath);
		} else if (OS.indexOf("nux") >= 0) {
			commandResult = executeCommand("."+File.separator+"diffcmds" + File.separator + "bsdiff.sh "
					+ oldApkFilePath + " " + newApkFilePath + " "
					+ patchFilePath);
		} else {
			// String
			// commandResult=executeCommand("."+File.separator+"bsdiff "+oldApkFilePath+" "+newApkFilePath
			// +" "+patchFilePath);
		}
	}

	private static String executeCommand(String command) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

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
