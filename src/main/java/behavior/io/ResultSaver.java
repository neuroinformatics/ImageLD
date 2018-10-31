package behavior.io;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.RoiDecoder;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import ij.text.TextPanel;

import behavior.Version;
import behavior.io.FileManager;
import behavior.io.FileCreate;
import behavior.io.SafetySaver;
import behavior.setup.Header;
import behavior.setup.Program;
import behavior.setup.parameter.Parameter;
import behavior.util.GetDateTime;

/***********************************
 *　実験の結果である数値や画像を統合的に保存するクラス。
 *　保存は、画像やXYデータなど各ケージ一つずつのものは、一つずつ指定して保存、
 *　一つのファイルに全ケージデータをいれるものは、一括保存にしている。
 ************************************/
/**
 * @author anonymous
 * @author Modifier Butoh
 */
public class ResultSaver{
	protected int allCage;
	protected Program program;
	private ImagePlus[] imageImp;
	private ImagePlus[] meanImp;
	private ImagePlus[] debugImp;
	protected ImageStack[] traceStack;
	private ImageStack[] imageStack;
	private ImageStack[] meanStack;
	private ImageStack[] debugStack;
	private ImageStack[] subtractStack;
	private ImageStack[] xorStack;
	protected String[] subjectID;
	protected boolean[] activeCage;
	protected FileManager fileManager = FileManager.getInstance();
	private boolean[] debugFlag;
	
	/******
	コンストラクタ。
	 *@param program プログラム番号。
	 *@param allCage 全ケージ数。
	 *@param backIp バックグラウンド画像
	 *******/
	public ResultSaver(Program program, int allCage, ImageProcessor[] backIp){
		this.program = program;
		this.allCage = allCage;
		debugFlag = new boolean[allCage];
		activeCage = new boolean[allCage];
		Arrays.fill(activeCage, true);
		imageImp = new ImagePlus[allCage];
		meanImp = new ImagePlus[allCage];
		debugImp = new ImagePlus[allCage];
		for(int cage = 0; cage < allCage; cage++) {
			imageImp[cage] = new ImagePlus("", backIp[cage]);
			meanImp[cage] = new ImagePlus("", backIp[cage]);
			debugImp[cage] = new ImagePlus("", backIp[cage]);
		}
		StackBuilder sb = new StackBuilder(allCage);
		imageImp = sb.buildStack(imageImp);
		meanImp = sb.buildStack(meanImp);
		debugImp = sb.buildStack(debugImp);
		imageStack = sb.getStack(imageImp);
		meanStack = sb.getStack(meanImp);
		debugStack = sb.getStack(debugImp);
		sb.deleteSlice(imageStack, 1);
		sb.deleteSlice(meanStack, 1);
		sb.deleteSlice(debugStack, 1);
		subtractStack = new ImageStack[allCage];
	    xorStack = new ImageStack[allCage];
	}

	public ImageStack[] getImageStack(){
		return imageStack;
	}

	/******
	トレース画像のセットアップ。トレースする際は、呼び出す必要がある。
	 *@param backIp バックグラウンド画像
	 *******/
	public void setTraceImage(ImageProcessor[] backIp){
		if(backIp.length != allCage)
			throw new IllegalArgumentException("");
		traceStack = new ImageStack[allCage];
		for(int cage = 0; cage < allCage; cage++){
			traceStack[cage] = (new ImagePlus("trace", backIp[cage])).createEmptyStack();
		}
	}

	public void setSubtractImage(ImageProcessor[] backIp){
		for(int cage = 0; cage < allCage; cage++){
			subtractStack[cage] = (new ImagePlus("subtract", backIp[cage])).createEmptyStack();
		}
	}

	public void setXorImage(ImageProcessor[] backIp){
		for(int cage = 0; cage < allCage; cage++){
			xorStack[cage] = (new ImagePlus("xor", backIp[cage])).createEmptyStack();
		}
	}
	/******
	トレース画像をスタックにつんでいく。
	 *@param cage スタックに積みたいケージの番号。
	 *******/
	public synchronized void addTraceImage(int cage, ImageProcessor traceIp){
		if(traceIp == null)
			return;
		traceStack[cage].addSlice("trace", traceIp.convertToByte(false)); //念のため、もう一度 ByteProcessor になおす処理をする。
	}

	public void addSubtractImage(int cage, ImageProcessor subIp){
		if(subIp == null)
			return;
		subtractStack[cage].addSlice("subtract", subIp.convertToByte(false)); //念のため、もう一度 ByteProcessor になおす処理をする。
	}

	public void addXorImage(int cage, ImageProcessor xorIp){
		if(xorIp == null)
			return;
		xorStack[cage].addSlice("xor", xorIp.convertToByte(false)); //念のため、もう一度 ByteProcessor になおす処理をする。
	}
	/******
	サブジェクトIDをセットする。これをしないと保存の際に差し支える。
	 *******/
	public void setSubjectID(String[] subjectID){
		if(subjectID.length != allCage)
			throw new IllegalArgumentException("cage num doesn't match:" + allCage + ":" + subjectID.length);
		this.subjectID = subjectID;
	}

	/******
	保存すべきケージを boolean[] の形でセットする。false になっている順番のケージについては保存が行われない。
	 *******/
	public void setActiveCage(boolean[] activeCage){
		this.activeCage = activeCage;
	}

	/******
	現在の画像をスタックに積む。
	 *@param cage ケージ番号。
	 *******/
	public synchronized void setCurrentImage(int cage, ImageProcessor currentIp){
		imageStack[cage].addSlice("image", currentIp.convertToByte(false)); //念のため、もう一度 ByteProcessor になおす処理をする。
	}

	/******
	画像を保存する。
	 *@param backIp バックグラウンド画像。
	 *******/
	public void saveImage(ImageProcessor[] backIp){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			imageStack[cage].addSlice("background", backIp[cage].convertToByte(false));
			SafetySaver saver = new SafetySaver();
			saver.saveImage(fileManager.getPaths(FileManager.imagePath)[cage], imageStack[cage]);
		}
	}
	
	public void setDebugFlag(boolean flag) {
		Arrays.fill(debugFlag, flag);
	}
	
	/******
	HC用。１日ごとの結果を保存
	 */
	public void saveResultPerHour(double[][] result, String path) {
		FileCreate creater = new FileCreate(path);
		
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			StringBuilder line = new StringBuilder(subjectID[cage]);
			for(int bin = 0; bin < result[cage].length; bin++)
				line.append("\t" + result[cage][bin]);
			creater.write(line.toString(), true);
		}
		
	}
	
	
	/******
	HC用。デバッグ用の画像を一定枚数積んでおく。
	 */
	public synchronized void setDebugImage(int cage, ImageProcessor currentIp, int sliceNum){
		if (debugFlag[cage] == false) {
			if (debugStack[cage].getSize() >= 5) {
				debugStack[cage].deleteSlice(1);
			}
		}
		debugStack[cage].addSlice("debug", currentIp.convertToByte(false));
		if (debugStack[cage].getSize() >= 10) {
			saveDebugImage(cage, sliceNum);
			while (debugStack[cage].getSize() > 0)
				debugStack[cage].deleteLastSlice();
			debugFlag[cage] = false;
		}
	}
	
	/******
	HC用。デバッグ用の画像を保存する。
	 *******/
	public void saveDebugImage(int cage, int sliceNum){
		String filename;
		
		SafetySaver saver = new SafetySaver();
		File dir = new File(fileManager.getPath(FileManager.ImagesDir));
		if(!dir.exists())
			dir.mkdir();
		filename = (fileManager.getPaths(FileManager.imagePath))[cage];
		filename = filename.substring(0, filename.length() - 4) + "_debug" + sliceNum + ".tif";      
		saver.saveImage(filename, debugStack[cage]);
		
	}
	
	/******
	HC用。平均画像をスタックに積む。
	 *******/
	public void setMeanImage(int cage){
		ImagePlus imp = new ImagePlus("", imageStack[cage]);
		ZProjector zp = new ZProjector(imp);
		zp.setMethod(ZProjector.AVG_METHOD);
		zp.setStartSlice(1);
		zp.setStopSlice(imp.getStackSize());
		zp.doProjection();
		
		meanStack[cage].addSlice("mean", zp.getProjection().getProcessor());
		while (imageStack[cage].getSize() > 0) { //forでsize分まわしてもうまくいかない。
			imageStack[cage].deleteLastSlice();
		}
	}
	
	/******
	HC用。平均画像を保存する。
	 *******/
	public void saveMeanImage(){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			SafetySaver saver = new SafetySaver();
			File dir = new File(fileManager.getPath(FileManager.ImagesDir));
			if(!dir.exists())
				dir.mkdir();
			saver.saveImage(fileManager.getPaths(FileManager.imagePath)[cage], meanStack[cage]);
		}
	}

	public void saveImage(ImageProcessor[] backIp,String session){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			imageStack[cage].addSlice("background", backIp[cage].convertToByte(false));
			SafetySaver saver = new SafetySaver();
			File imageFile = new File(fileManager.getPath(FileManager.ImagesDir)+File.separator+session);
			if(!imageFile.exists())
				imageFile.mkdir();
			saver.saveImage(fileManager.getPath(FileManager.ImagesDir)+File.separator+session+File.separator+subjectID[cage]+".tif", imageStack[cage]);
		}
	}

	/******
	トレース画像の保存。
	 *******/
	public synchronized void saveTraceImage(){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			if(traceStack[cage].getSize() == 0) continue;
			SafetySaver saver = new SafetySaver();
			File traceFile = new File(fileManager.getPath(FileManager.TracesDir));
			if (!traceFile.exists())
				traceFile.mkdir();
			saver.saveImage(fileManager.getPaths(FileManager.tracePath)[cage], traceStack[cage]);
		}
	}

	public void saveOfflineTraceImage(){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			if(traceStack[cage].getSize() == 0) continue;
			SafetySaver saver = new SafetySaver();
			File traceFile = new File(fileManager.getSavePath(FileManager.TracesDir));
			if (!traceFile.exists())
				traceFile.mkdir();
			saver.saveImage(fileManager.getSavePaths(FileManager.tracePath)[cage], traceStack[cage]);
		}
	}

	public synchronized void saveTraceImage(String session){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			if(traceStack[cage].getSize() == 0) continue;
			SafetySaver saver = new SafetySaver();
			File traceFile = new File(fileManager.getPath(FileManager.TracesDir)+File.separator+session);
			if(!traceFile.exists())
				traceFile.mkdir();
			saver.saveImage(fileManager.getPath(FileManager.TracesDir) +File.separator+session+File.separator + subjectID[cage]+".tif", traceStack[cage]);
		}
	}

	public void saveSubtractImage(){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			if(subtractStack[cage].getSize() == 0) continue;
			SafetySaver saver = new SafetySaver();
			saver.saveImage(fileManager.getPath(FileManager.ImagesDir)+File.separator+subjectID[cage]+"_subtract.tif", subtractStack[cage]);
		}
	}

	public void saveXorImage(){
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			if(xorStack[cage].getSize() == 0) continue;
			SafetySaver saver = new SafetySaver();
			saver.saveImage(fileManager.getPath(FileManager.ImagesDir)+File.separator+subjectID[cage]+"_xor.tif", xorStack[cage]);
		}
	}
	/******
	XY データの保存。
	 *@param cage ケージ番号。
	 *@param xyTp 保存するテキストパネル。
	 *******/
	public void saveXYResult(int cage, TextPanel xyTp){
		File xyFile = new File(fileManager.getPath(FileManager.XY_DataDir));
		if (!xyFile.exists())
			xyFile.mkdir();
		xyTp.saveAs(fileManager.getPaths(FileManager.xyPath)[cage]);
	}

	public void saveXYResult(int cage, TextPanel xyTp,String session){
		File xyFile = new File(fileManager.getPath(FileManager.XY_DataDir)+File.separator+session);
		if(!xyFile.exists())
			xyFile.mkdir();
		xyTp.saveAs(fileManager.getPath(FileManager.XY_DataDir) + File.separator + session + File.separator+subjectID[cage]+".txt");
	}

	public void saveXYResult(int cage,List<String> text){
		File xyFile = new File(fileManager.getPath(FileManager.XY_DataDir));
		if (!xyFile.exists())
			xyFile.mkdir();

		FileCreate creater = new FileCreate(fileManager.getPaths(FileManager.xyPath)[cage]);
		creater.write("", false);
		creater.write(Header.getXYHeader(program), true);
		for(String s:text){
		    creater.write(s, true);
		}
	}

	public void saveOfflineXYResult(int cage, TextPanel xyTp){
		File xyFile = new File(fileManager.getSavePath(FileManager.XY_DataDir));
		if (!xyFile.exists())
			xyFile.mkdir();
		xyTp.saveAs(fileManager.getSavePaths(FileManager.xyPath)[cage]);
	}

	public void saveOfflineXYResult(int cage, TextPanel xyTp,String session){
		File xyFile = new File(fileManager.getSavePath(FileManager.XY_DataDir)+File.separator+session);
		if(!xyFile.exists())
			xyFile.mkdir();
		xyTp.saveAs(fileManager.getSavePath(FileManager.XY_DataDir) + File.separator + session + File.separator+subjectID[cage]+".txt");
	}

	public void saveOfflineXYResult(int cage,List<String> text){
		File xyFile = new File(fileManager.getSavePath(FileManager.XY_DataDir));
		if (!xyFile.exists())
			xyFile.mkdir();

		FileCreate creater = new FileCreate(fileManager.getSavePaths(FileManager.xyPath)[cage]);
		creater.write("", false);
		creater.write(Header.getXYHeader(program), true);
		for(String s:text){
		    creater.write(s, true);
		}
	}

	/******
	トータルデータの保存。
	 *@param result result[ケージ番号][データの種類]。
	 *@param writeHeader ヘッダを書くかどうか。
	 *******/
	public void saveOnlineTotalResult(String[][] result,Calendar[] calendar, boolean writeHeader, boolean writeVersion, boolean writeParameter){
		FileCreate creater = new FileCreate(fileManager.getPath(FileManager.totalResPath));
		if(writeHeader){
			String header = Header.getTotalResultHeader(program); 
			creater.write(header+"\tExperimentDate(MMDDYY)\tExperimentTime(HH:MM:SS)", true);
		}
		if(writeVersion){
			creater.write("#Online, " + Version.getVersion() + ", ImageJ" + ij.IJ.getVersion(), true);
		}
		if(writeParameter){
			saveParameter(creater);
		}
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			StringBuilder line = new StringBuilder(250);
			line.append(subjectID[cage]);
			for(int bin = 0; bin < result[cage].length; bin++){
				line.append("\t" + result[cage][bin]);
			}
			line.append("\t");
			line.append(((calendar[cage].get(Calendar.MONTH)+1)<10?"0":"")+(calendar[cage].get(Calendar.MONTH)+1));
			line.append(((calendar[cage].get(Calendar.DAY_OF_MONTH))<10?"0":"")+calendar[cage].get(Calendar.DAY_OF_MONTH));
			line.append((new String(calendar[cage].get(Calendar.YEAR)+"")).substring(2));
			line.append("\t");
			if(calendar[cage].get(Calendar.AM_PM)==Calendar.AM){
			    line.append((((calendar[cage].get(Calendar.HOUR))<10)?"0":"")+calendar[cage].get(Calendar.HOUR));
			}else{
				line.append((calendar[cage].get(Calendar.HOUR)+12));
			}
			line.append(":");
			line.append(((calendar[cage].get(Calendar.MINUTE))<10?"0":"")+calendar[cage].get(Calendar.MINUTE));
			line.append(":");
			line.append(((calendar[cage].get(Calendar.SECOND))<10?"0":"")+calendar[cage].get(Calendar.SECOND));
			creater.write(line.toString(), true);
		}
	}

	/******
	トータルデータの保存。
	 *@param result result[ケージ番号][データの種類]。
	 *@param writeHeader ヘッダを書くかどうか。
	 *******/
	public void saveOfflineTotalResult(String[] result, boolean writeHeader, boolean writeVersion, boolean writeParameter){
		File file = new File(fileManager.getSavePath(FileManager.ResultsDir));
		if (!file.exists()){
			file.mkdir();
	    }

		FileCreate creater = new FileCreate(fileManager.getSavePath(FileManager.totalResPath));
		if(writeHeader){
			String header = Header.getTotalResultHeader(program); 
			creater.write(header, true);
		}
		if(writeVersion){
			creater.write("#Offline, " + Version.getVersion() + ", ImageJ" + ij.IJ.getVersion(), true);
		}
		if(writeParameter){
			saveParameter(creater);
		}
		StringBuffer line = new StringBuffer(subjectID[0]);
		for(int bin = 0; bin < result.length; bin++){
			line.append("\t" + result[bin]);
		}
		creater.write(line.toString(), true);
	}

	protected void saveParameter(FileCreate creater){
		BufferedReader reader = null;
		try{
		    reader = new BufferedReader(new FileReader(FileManager.getInstance().getPath(FileManager.parameterPath)));

		    String line = "";
	        while((line = reader.readLine()) != null){
		        creater.write("##"+line, true);
	        }
		}catch(IOException e){
			e.printStackTrace();
		}

	    File path = new File(FileManager.getInstance().getPath(FileManager.PreferenceDir));
		File[] list = path.listFiles(new FileFilter(){
			public boolean accept(File pathname){
				String name = pathname.getName();
				if(name.length() >= 4 && name.substring(name.length()-4).equals(".roi")){	// .roi なファイルのみ受け付ける
					if(program==Program.CSI || program==Program.OLDCSI || program==Program.YM){
						if(name.length() >= 9 && name.substring(name.length()-9).equals("Outer.roi"))
							return false;
					}
					return true;
				}else{
					return false;
				}
			}
		});

		if(list.length!=0){
			creater.write("####ROI(RoiName=x\ty\twidth\theight)", true);
		}

		for(int i=0;i<list.length;i++){
			Roi bufRoi = null;
			try{
		        bufRoi = new RoiDecoder(list[i].getPath()).getRoi();
			}catch(IOException e){
				continue;
			}
		    final Rectangle bufrec = bufRoi.getBounds();
	        creater.write("##"+list[i].getName()+"="+bufrec.x+"\t"+bufrec.y+"\t"+bufrec.width+"\t"+bufrec.height, true);
		}
	}

	/******
	bin（durationをいくつかに分割した単位）データの保存。
	 *@param fileName 保存するファイル名。
	 *@param result result[ケージ番号][bin]。
	 *@param binHeader ヘッダに、"bin" とつけていくか。
	 *******/
	public void saveOnlineBinResult(String fileName, String[][] result, boolean writeHeader, boolean binHeader, boolean writeVersion){
		String binResultPath = fileManager.getBinResultPath(fileName);
		FileCreate creater = new FileCreate(binResultPath);
		if(writeHeader){
			StringBuilder header = new StringBuilder("ID");
			int binLength = Parameter.getInt(Parameter.duration) / Parameter.getInt(Parameter.binDuration);
			/* bin に分けると端数がでるようなら、bin の長さを一つ長くする必要がある*/
			if(Parameter.getInt(Parameter.duration) % Parameter.getInt(Parameter.binDuration) != 0)
				binLength++;
			for(int bin = 0; bin < binLength; bin++)
				header.append("\t" + (binHeader? "bin" : "") + (bin + 1));
			creater.write(header.toString(), true);
		}
		if(writeVersion){
			creater.write("#Online, " + Version.getVersion() + ", ImageJ" + ij.IJ.getVersion(), true);
		}
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage]){
				//creater.write("# null", true);
				continue;
			}
			StringBuilder binResult = new StringBuilder(subjectID[cage]);
			for(int bin = 0; bin < result[cage].length; bin++)
				binResult.append("\t" + result[cage][bin]);
			creater.write(binResult.toString(), true);
		}
	}

	/******
	bin（durationをいくつかに分割した単位）データの保存。
	saveBinResultは2つあるが、bin用のヘッダーをつけない場合はこちら。
	 *@param fileName 保存するファイル名。
	 *@param result result[ケージ番号][bin]。
	 *******/
	public void saveOnlineBinResult(String fileName, String[][] result, boolean writeHeader, boolean writeVersion){
		saveOnlineBinResult(fileName, result, writeHeader, true,writeVersion);
	}

	/**
	 * binを使用しないBT,RM用
	 * やってることはsaveBinResult()とそう変わらない
	 * @param FileName 結果ファイル名
	 * @param headerName　ヘッダ名
	 * @param headerNum　ヘッダの個数
	 * @param result　結果
	 * @param writeHeader　ヘッダを記入するか
	 */
	public void saveOnlineRepectiveResults(String FileName, String headerName, int headerNum, List<String> result, boolean writeHeader, boolean writeVersion){
		String respectiveResultsPath = fileManager.getBinResultPath(FileName);
		FileCreate creater = new FileCreate(respectiveResultsPath);
		//ヘッダの記入
		if(writeHeader){
			StringBuilder header = new StringBuilder("TrialName");
			for(int i= 0; i < headerNum; i++)
				header.append("\t" + headerName + (i+1));
			creater.write(header.toString(), true);
		}
		if(writeVersion){
			creater.write("#Online, " + Version.getVersion() + ", ImageJ" + ij.IJ.getVersion(), true);
		}
		//結果の記入
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			//このあたりのCage数への対応はまだ不完全
			//(BT,RMではCage数を1に固定している）
			StringBuffer respectiveResults = new StringBuffer(subjectID[cage]);
			for(Iterator<String> iter= result.iterator();iter.hasNext();){
				respectiveResults.append("\t" + iter.next());
			    iter.remove();
	 	    }
			creater.write(respectiveResults.toString(), true);
		}
	}

	/******
	日時を結果ファイルの末尾に記入する
	 *@param writeTotal total結果ファイルに記入するか
	 *@param writeBin bin結果ファイルに記入するか
	 *@param binFileName bin結果ファイルに記入する際、その名前
	 */
	public void writeDate(boolean writeTotal, boolean writeBin, String[] binFileName){
		FileCreate create = new FileCreate();
		if(writeTotal)
			create.writeDate(fileManager.getPath(FileManager.totalResPath));
		if(writeBin)
			for(int i = 0; i < binFileName.length; i++)
				create.writeDate(fileManager.getBinResultPath(binFileName[i]));
	}

	/**
	 */
	public void writeDate(String[] binFileName){
		writeDate(true, true, binFileName);
	}

	public void writeDateOffline(boolean writeTotal, boolean writeBin, String[] binFileName){
		FileCreate create = new FileCreate();
		if(writeTotal)
			create.writeDate(fileManager.getSavePath(FileManager.totalResPath));
		if(writeBin)
			for(int i = 0; i < binFileName.length; i++)
				create.writeDate(fileManager.getSaveBinResultPath(binFileName[i]));
	}

	/**
	 * HC用
	 * 開始・終了日時を結果ファイルの末尾に記入する。
	 * @param startTime 開始日時
	 */
	public void writeDateWithStartTime(String startTime){
		FileCreate create = new FileCreate();
		String date = GetDateTime.getInstance().getDateTimeString();
		String writeString = startTime + " - " + date;
		create.writeChar(fileManager.getPath(FileManager.totalResPath), writeString, true);
	}

	public void saveOfflineBinResult(String fileName, String[][] result, boolean writeHeader, boolean binHeader, boolean writeVersion){
		String binResultPath = fileManager.getSaveBinResultPath(fileName);
		FileCreate creater = new FileCreate(binResultPath);
		if(writeHeader){
			StringBuilder header = new StringBuilder("ID");
			int binLength = Parameter.getInt(Parameter.duration) / Parameter.getInt(Parameter.binDuration);
			/* bin に分けると端数がでるようなら、bin の長さを一つ長くする必要がある*/
			if(Parameter.getInt(Parameter.duration) % Parameter.getInt(Parameter.binDuration) != 0)
				binLength++;
			for(int bin = 0; bin < binLength; bin++)
				header.append("\t" + (binHeader? "bin" : "") + (bin + 1));
			creater.write(header.toString(), true);
		}
		if(writeVersion){
			creater.write("#Offline, " + Version.getVersion() + ", ImageJ" + ij.IJ.getVersion(), true);
		}
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage]){
				//creater.write("# null", true);
				continue;
			}
			StringBuilder binResult = new StringBuilder(subjectID[cage]);
			for(int bin = 0; bin < result[cage].length; bin++)
				binResult.append("\t" + result[cage][bin]);
			creater.write(binResult.toString(), true);
		}
	}

	/******
	bin（durationをいくつかに分割した単位）データの保存。
	saveBinResultは2つあるが、bin用のヘッダーをつけない場合はこちら。
	 *@param fileName 保存するファイル名。
	 *@param result result[ケージ番号][bin]。
	 *******/
	public void saveOfflineBinResult(String fileName, String[][] result, boolean writeHeader, boolean writeVersion){
		saveOfflineBinResult(fileName, result, writeHeader, true,writeVersion);
	}

	/**
	 * binを使用しないBT,RM用
	 * やってることはsaveBinResult()とそう変わらない
	 * @param FileName 結果ファイル名
	 * @param headerName　ヘッダ名
	 * @param headerNum　ヘッダの個数
	 * @param result　結果
	 * @param writeHeader　ヘッダを記入するか
	 */
	public void saveOfflineRepectiveResults(String FileName, String headerName, int headerNum, List<String> result, boolean writeHeader, boolean writeVersion){
		String respectiveResultsPath = fileManager.getSaveBinResultPath(FileName);
		FileCreate creater = new FileCreate(respectiveResultsPath);
		//ヘッダの記入
		if(writeHeader){
			StringBuilder header = new StringBuilder("TrialName");
			for(int i= 0; i < headerNum; i++)
				header.append("\t" + headerName + (i+1));
			creater.write(header.toString(), true);
		}
		if(writeVersion){
			creater.write("#Offline, " + Version.getVersion() + ", ImageJ" + ij.IJ.getVersion(), true);
		}
		//結果の記入
		for(int cage = 0; cage < allCage; cage++){
			if(!activeCage[cage])
				continue;
			//このあたりのCage数への対応はまだ不完全
			//(BT,RMではCage数を1に固定している）
			StringBuffer respectiveResults = new StringBuffer(subjectID[cage]);
			for(Iterator<String> iter= result.iterator();iter.hasNext();){
				respectiveResults.append("\t" + iter.next());
			    iter.remove();
	 	    }
			creater.write(respectiveResults.toString(), true);
		}
	}
}