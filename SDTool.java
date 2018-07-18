package kgg.sdtool;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class SDTool extends JPanel {
	private static String clipboard;
	private final static String directory = System.getProperty("user.home").replace("\\","/")+"/.switch";
	private static Stage stage=Stage.credits;
	private static HashMap<String, String> biskeyList = new HashMap<>();
	private static HashMap<String, String> titlekeyList = new HashMap<>();
	private static String eticket;
	private static String sdSeed;
	private static JFrame frame;
	private static String defaultDrive;
	enum Stage {
		credits,
		biskeys,

		sd_key_private,
		sd_key_get,

		title_key_eticket,
		title_key_prod,
		title_key_save,
		title_key_make_blobs,

		decrypting,
	}
	public static void main(String[] args) {
		new SDTool();
	}
	private static void log(String s) { System.out.println(s); }
	private static void pop(String s) {
		JOptionPane.showMessageDialog(frame, s);
	}
	private SDTool() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 400);
		frame.setLocationRelativeTo(null);
		frame.setTitle("SDTool");
		frame.add(this);
		frame.setVisible(true);
		preCheck();
		loadStage();
		this.setLayout(new CardLayout());



		/*Loading*/
		JPanel panelLoading = new JPanel();
		panelLoading.add(new JLabel("Loading..."));
		this.add(panelLoading, "loading");


		/*credits*/
		JPanel panelCredits = new JPanel();
		this.add(panelCredits, "credits");
		panelCredits.setLayout(new BoxLayout(panelCredits, BoxLayout.PAGE_AXIS));
		panelCredits.add(new JLabel("Credits/Thanks:"));
		panelCredits.add(new JLabel(""));
		panelCredits.add(new JLabel("This Tool: TheKgg"));
		panelCredits.add(new JLabel("Memloader: rajkosto"));
		panelCredits.add(new JLabel("TegraRcmSmash: rajkosto"));
		panelCredits.add(new JLabel("BiskeyDump: rajkosto"));
		panelCredits.add(new JLabel("Python Scripts: ?"));
		panelCredits.add(new JLabel("The original guide this is based off of: Khangaroo"));
		JButton start = new JButton("Start");
		start.addActionListener(ae -> {
			stage=Stage.biskeys;
			loadCard();
		});
		panelCredits.add(start);



		/*Biskeys GUI*/
		JPanel panelBiskeys = new JPanel();
		this.add(panelBiskeys, "biskeys");
		panelBiskeys.setLayout(new BoxLayout(panelBiskeys, BoxLayout.PAGE_AXIS));
		panelBiskeys.add(new JLabel("Step 1.0: Getting biskeys"));
		panelBiskeys.add(new JLabel("You will need to get into RCM multiple times. It is highly advised to turn on AutoRCM for this."));
		panelBiskeys.add(new JLabel("Please get your Switch into RCM and plugged into your computer. (with drivers installed)"));
		final JLabel qr = new JLabel("Once the QR appears on your Switch, please tap the power button.");
		qr.setVisible(false);

		JButton getBiskeys = new JButton("Ready");
		panelBiskeys.add(getBiskeys);
		getBiskeys.addActionListener( ae -> {
			qr.setVisible(true);
			Thread t = new Thread(() -> {
				if (getBiskeys()) {
					stage = Stage.sd_key_private;
					loadCard();
				} else {
					qr.setVisible(false);
				}
			});
			t.start();
		});
		panelBiskeys.add(qr);

		/*sd seed private GUI*/
		JPanel panelSdSeedPrivate = new JPanel();
		this.add(panelSdSeedPrivate, "sd_key_private");
		panelSdSeedPrivate.setLayout(new BoxLayout(panelSdSeedPrivate, BoxLayout.PAGE_AXIS));
		panelSdSeedPrivate.add(new JLabel("Step 2.0: Getting SD seed [contents of private]"));
		panelSdSeedPrivate.add(new JLabel("Tap the power button (to get your Switch back into RCM)"));
		panelSdSeedPrivate.add(new JLabel("If you have used memloader before, just tap the power button to go into USB command mode. If you haven't, just wait a few seconds."));
		panelSdSeedPrivate.add(new JLabel("If nothing happens when you click Ready, double check your switch is in RCM, and click it again."));
		JButton sdSeedGetPrivate = new JButton("Ready");
		panelSdSeedPrivate.add(sdSeedGetPrivate);
		sdSeedGetPrivate.addActionListener(ae -> {
			if(smashFailed("\"" + directory + "/SDTool/payloads/memloader.bin\" --dataini=\"" + directory + "/SDTool/payloads/memloader/ums_sd.ini\"")) {
				pop("Failed to run TegraRcmSmash. (4)");
				return;
			}
			File[] drives = File.listRoots();
			Thread t = new Thread(() -> {
				while(true) {
					try { Thread.sleep(10); } catch (InterruptedException ignored) { }
					File[] d = File.listRoots();
					if (d.length > drives.length) {
						for(File dr : d) {
							boolean there = false;
							for (File dr1 : drives)
								if(dr1.equals(dr))
									there=true;
							if(!there && loadPrivate(dr.toString().substring(0,1))) stage = Stage.sd_key_get;
						}
						loadCard();
						break;
					}
				}
			});
			t.start();
		});

		/*sd seed get GUI*/
		JPanel panelSdSeedGet = new JPanel();
		this.add(panelSdSeedGet, "sd_key_get");
		panelSdSeedGet.setLayout(new BoxLayout(panelSdSeedGet, BoxLayout.PAGE_AXIS));
		panelSdSeedGet.add(new JLabel("Step 2.1: Getting SD seed [mounting SYSTEM]"));
		panelSdSeedGet.add(new JLabel("If Windows asks to format a disk (it shouldn't), DO NOT DO IT. IT CAN PERMANENTLY BRICK."));
		panelSdSeedGet.add(new JLabel("Hold down the power button for 12 seconds (until the screen turns off), wait a second, get it back into RCM, and click Ready:"));
		JButton sdSeedMount = new JButton("Ready");
		panelSdSeedGet.add(sdSeedMount);
		sdSeedMount.addActionListener(ae -> {
			if(smashFailed("\"" + directory + "/SDTool/payloads/memloader.bin\" --dataini=\"" + directory + "/SDTool/payloads/memloader/ums_emmc.ini\"")) {
				pop("Failed to run TegraRcmSmash. (4)");
			}
		});
		panelSdSeedGet.add(new JLabel("Once your Switch's screen is on, open HacDiskMount as Administrator."));
		panelSdSeedGet.add(new JLabel("Click File>Open physical drive>Linux UMS disk (29.121 GiB), then click OK (name and size MUST match)"));
		panelSdSeedGet.add(new JLabel("Double click on SYSTEM. Fill in the correct biskeys by copying them using the buttons below:"));

		JButton copyUpper2 = new JButton("Copy Crypto (Upper)");
		panelSdSeedGet.add(copyUpper2);
		copyUpper2.addActionListener(ae->setClip(biskeyList.get("2c")));
		JButton copyLower2 = new JButton("Copy Tweak (Lower)");
		panelSdSeedGet.add(copyLower2);
		copyLower2.addActionListener(ae->setClip(biskeyList.get("2t")));

		panelSdSeedGet.add(new JLabel("Click test, then save. If there was an error testing you have copied the wrong keys."));
		panelSdSeedGet.add(new JLabel("If it says \"Drivers are not installed,\" click the Install button."));
		panelSdSeedGet.add(new JLabel("Click Mount in HacDiskMount, then click Done below (this will take a few seconds):"));
		JButton sdGetSave = new JButton("Done");
		panelSdSeedGet.add(sdGetSave);
		sdGetSave.addActionListener(ae -> {
			try {
				File[] r = File.listRoots();
				for(File root : r) {
					if(new File(root.toString()+"/save/8000000000000043").exists()) {
						if(loadSave(root.toString().substring(0,1))) {
							stage=Stage.title_key_eticket;
							loadCard();
							return;
						} else {
							pop("Failed. (7)");
						}
						break;
					}
				}
				pop("It looks like you don't have your Switch's SYSTEM partition mounted.");
			} catch (Exception e) {
				pop("Failed. (8)");
				e.printStackTrace();
			}
		});


		/*get eticket*/
		JPanel panelGetEticket = new JPanel();
		this.add(panelGetEticket, "title_key_eticket");
		panelGetEticket.setLayout(new BoxLayout(panelGetEticket, BoxLayout.PAGE_AXIS));
		panelGetEticket.add(new JLabel("Step 3.0: Getting title keys [eticket]"));
		panelGetEticket.add(new JLabel("You need to get the Switch's eticket_rsa_kek."));
		JButton eticketGot = new JButton("I got it");
		panelGetEticket.add(eticketGot);
		eticketGot.addActionListener( ae -> {
			String eticket = JOptionPane.showInputDialog("Paste it here:");
			if(eticket==null) {
				pop("dont close the popup D:<");
				return;
			}
			eticket=eticket.replace(" ", "").toUpperCase();
			if(eticket.startsWith("1") && eticket.endsWith("4") && eticket.length()==32) {
				writeToFile(directory+"/eticket.txt", eticket);
				SDTool.eticket=eticket;
				stage=Stage.title_key_prod;
				loadCard();
			} else {
				pop("Your eticket is incorrect.");
			}
		});


		/*title key prod*/
		JPanel panelGetProd = new JPanel();
		this.add(panelGetProd, "title_key_prod");
		panelGetProd.setLayout(new BoxLayout(panelGetProd, BoxLayout.PAGE_AXIS));
		panelGetProd.add(new JLabel("Step 3.1 (a): Getting title keys [backing up PRODINFO]"));
		panelGetProd.add(new JLabel("Go back to HacDiskMount, and click the Unmount button. (this will take a few seconds)"));
		panelGetProd.add(new JLabel("Close out of the SYSTEM window, then double click PRODINFO."));
		panelGetProd.add(new JLabel("Fill in the biskeys by copying them using the buttons and pasting them into their boxes:"));
		panelGetProd.add(new JLabel("Note: these keys are not the same as last time"));
		JButton copyUpper0 = new JButton("Copy Crypto (Upper)");
		panelGetProd.add(copyUpper0);
		copyUpper0.addActionListener(ae->setClip(biskeyList.get("0c")));
		JButton copyLower0 = new JButton("Copy Tweak (Lower)");
		panelGetProd.add(copyLower0);
		copyLower0.addActionListener(ae->setClip(biskeyList.get("0t")));
		panelGetProd.add(new JLabel("Click test, then save. If the test failed you copied the wrong keys."));
		panelGetProd.add(new JLabel("In the \"Dump to file\" section, click Browse, go to your Desktop, and click Save. In the same section, click Start."));
		panelGetProd.add(new JLabel("Once this is finished, click Ready:"));
		JButton getProd = new JButton("Ready");
		panelGetProd.add(getProd);
		getProd.addActionListener(ae->{
			File prod = new File(System.getProperty("user.home").replace("\\","/")+"/Desktop/PRODINFO.bin");
			if(prod.exists()) {
				try {
					Files.move(prod.toPath(), Paths.get(directory+"/SDTool/PRODINFO.bin"));
				} catch (Exception e) {
					e.printStackTrace();
					pop("Failed moving PRODINFO.bin.");
				} finally {
					stage = Stage.title_key_make_blobs;
					loadCard();
				}
			}
		});

		/*title key save*/
		/*This is skipped the first time so it is also 3.1*/
		JPanel panelGetSave = new JPanel();
		this.add(panelGetSave, "title_key_save");
		panelGetSave.setLayout(new BoxLayout(panelGetSave, BoxLayout.PAGE_AXIS));
		panelGetSave.add(new JLabel("Step 3.1 (b): Getting title keys [Getting saves from Switch]"));
		panelGetSave.add(new JLabel("Get your Switch into RCM and plugged into your PC, then click Ready:"));
		JButton sdSeedMount1 = new JButton("Ready");
		panelGetSave.add(sdSeedMount1);
		sdSeedMount1.addActionListener(ae -> {
			if(smashFailed("\"" + directory + "/SDTool/payloads/memloader.bin\" --dataini=\"" + directory + "/SDTool/payloads/memloader/ums_emmc.ini\"")) {
				pop("Failed to run TegraRcmSmash. (4)");
			}
		});
		panelGetSave.add(new JLabel("Once your Switch's screen is on, open HacDiskMount as Administrator."));
		panelGetSave.add(new JLabel("Click File>Open physical drive>Linux UMS disk (29.121 GiB), then click OK (name and size MUST match)"));
		panelGetSave.add(new JLabel("Double click SYSTEM, then click Mount. Once this is mounted, click Done:"));
		JButton titleGetSave = new JButton("Done");
		panelGetSave.add(titleGetSave);
		titleGetSave.addActionListener(ae -> {
			try {
				File[] r = File.listRoots();
				for(File root : r) {
					if(new File(root.toString()+"/save/8000000000000043").exists()) {
						if(loadTitleSave(root.toString().substring(0,1))) {
							stage=Stage.title_key_make_blobs;
							loadCard();
						}
						return;
					}
				}
				pop("It looks like you don't have your Switch's SYSTEM partition mounted.");
			} catch (Exception e) {
				pop("Failed. (8)");
				e.printStackTrace();
			}
		});

		/*title_key_make_blobs*/
		JPanel panelMakeBlobs = new JPanel();
		this.add(panelMakeBlobs, "title_key_make_blobs");
		panelMakeBlobs.setLayout(new BoxLayout(panelMakeBlobs, BoxLayout.PAGE_AXIS));
		panelMakeBlobs.add(new JLabel("Step 3.2: Getting title keys [actually making them]"));
		panelMakeBlobs.add(new JLabel("We have all of the files necessary now. Click Ready to start:"));
		JButton makeBlobs = new JButton("Ready");
		panelMakeBlobs.add(makeBlobs);
		makeBlobs.addActionListener(ae-> {
			try {
				Path p = Paths.get(directory + "/SDTool/scripts/get_titlekeys.py");
				List<String> lines = Files.readAllLines(p);
				lines.set(9, "rsa_kek = uhx('"+eticket+"')");
				Files.write(p, lines);
			} catch(Exception e) {
				pop("Failed to put the eticket into the python script. (11)");
				return;
			}
			if(!pyscript("get_ticketbins.py 80000000000000e1").isEmpty() ) {
				if(!pyscript("get_ticketbins.py 80000000000000e2").isEmpty()) {
					String titlekeys= pyscript("get_titlekeys.py \""+directory+"/SDTool/PRODINFO.bin\" personal_ticketblob.bin");
					String titlekeysCommon = pyscript("get_titlekeys.py \""+directory+"/SDTool/PRODINFO.bin\" common_ticketblob.bin");
					log(titlekeys + " : " + titlekeysCommon);
					titlekeys+="\n"+titlekeysCommon;
					List<String> keyLines = Arrays.asList(titlekeys.split("\n"));
					StringBuilder formattedKeys = new StringBuilder();
					for(String keyLine : keyLines) {
						if(keyLine.contains("Title ID")) {
							formattedKeys.append(keyLine.split(":")[1].replace(" ","").toUpperCase());
							formattedKeys.append(":");
							formattedKeys.append(keyLines.get(keyLines.indexOf(keyLine)+1).split(":")[1].replace(" ","").toUpperCase());
							formattedKeys.append("\n");
						}
					}
					log(formattedKeys.toString());
					if(!writeToFile(directory+"/SDTool/title_keys.txt", formattedKeys.toString())) {
						pop("Failed to write title keys to file.");
						return;
					}
					loadTitleKeys();
				} else {
					pop("Failed getting ticket bins for e2.");
					return;
				}
			} else {
				pop("Failed getting ticket bins for e1.");
				return;
			}
			try {
				Files.deleteIfExists(Paths.get(directory+"/SDTool/scripts/80000000000000e2"));
				Files.deleteIfExists(Paths.get(directory+"/SDTool/scripts/80000000000000e1"));
			} catch (IOException e) {
				e.printStackTrace();
				pop("Failed to clear out unneeded files. ");
			}
			stage=Stage.decrypting;
			loadCard();
		});

		/*decrypting*/
		JPanel panelDecrypting = new JPanel();
		this.add(panelDecrypting, "decrypting");
		panelDecrypting.setLayout(new BoxLayout(panelDecrypting, BoxLayout.PAGE_AXIS));
		panelDecrypting.add(new JLabel("Step 4: Decrypting your Games"));
		panelDecrypting.add(new JLabel("Hold down power until your switch turns off completely, then get it back into RCM, then click Ready:"));
		JButton mountSd = new JButton("Ready");
		panelDecrypting.add(mountSd);
		mountSd.addActionListener(ae -> {
			if(smashFailed("\"" + directory + "/SDTool/payloads/memloader.bin\" --dataini=\"" + directory + "/SDTool/payloads/memloader/ums_sd.ini\"")) {
				pop("Failed to run TegraRcmSmash. (4)");
				return;
			}
			File[] drives = File.listRoots();
			Thread t = new Thread(() -> {
				while(true) {
					try { Thread.sleep(10); } catch (InterruptedException ignored) { }
					File[] d = File.listRoots();
					if (d.length > drives.length) {
						for(File dr : d) {
							boolean there = false;
							for (File dr1 : drives)
								if(dr1.equals(dr))
									there=true;
							if(!there) {
								File[] fs=dr.listFiles((dir, name) -> name.toLowerCase().endsWith(".ini_d"));
								if(fs!=null && fs.length>0)
								for(File ini : fs) {
									try {
										Files.move(ini.toPath(), Paths.get(ini.toString().split(Pattern.quote("."))[0]+".ini"));
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								clipboard=dr.toString().substring(0,1);
								pop("Ready.\nI also fixed your ini files.");
							}
						}
						loadCard();
						break;
					}
				}
			});
			t.start();
		});
		panelDecrypting.add(new JLabel("Once you have gotten a dialog saying \"Ready,\" continue reading:"));
		panelDecrypting.add(new JLabel("Clicking \"Find file\" will bring up a file chooser for you to pick the file you want to decrypt."));
		panelDecrypting.add(new JLabel("Problem is, Nintendo saves file names differently on the SD card. You must use a combination of"));
		panelDecrypting.add(new JLabel("checking the filesizes, date modified, and friends, to figure out which rom you want to decrypt."));
		panelDecrypting.add(new JLabel("You MUST select a file on the SD card in the correct place that the Switch uses, or else this won't work."));
		panelDecrypting.add(new JLabel("The file will be called \"00.\""));
		panelDecrypting.add(new JLabel("Once you select a file, you will need to tell SDTool where you want to save it. (it will be an NCA)"));
		JButton findFileToDecrypt = new JButton("Find file");
		panelDecrypting.add(findFileToDecrypt);
		findFileToDecrypt.addActionListener(ae->{
			JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File(clipboard+":/Nintendo/Contents/registered/"));
			if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
				String rom = fc.getSelectedFile().toString().replace("\\", "/");
				JFileChooser saveAs = new JFileChooser();
				saveAs.setFileFilter(new FileNameExtensionFilter("NCA", "nca"));
				if(saveAs.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					pop("Decryption will start when you click \"OK.\" This may take a while...");
					String out = hactool("-t nax0 --sdseed="+sdSeed+" --sdpath=\""+rom.substring(rom.indexOf("register")-1, rom.length()-3)+"\" --plaintext=\""+saveAs.getSelectedFile().toString().replace("\\", "/")+"\" \""+rom+"\"");
					if(out.isEmpty()) {
						pop("Failed to run hactool. Please try again.");
					} else {
						pop("Done saving the NCA. You have one final step to do: get your TitleID. Clicking OK will bring you to a website that you can find this on.");
						try {
							Desktop.getDesktop().browse(new URI("http://switchbrew.org/index.php?title=Title_list/Games"));
						} catch(Exception e) {
							e.printStackTrace();
							pop("Failed opening the link, just Google \"Switch TitleIDs.\"");
						}
						String titleid=null;
						while(titleid==null) {
							titleid = JOptionPane.showInputDialog("Your TitleID:");
							if(titleid!=null && titlekeyList.getOrDefault(titleid.toUpperCase(), "ded").equals("ded")) {
								pop("Your TitleID is incorrect.");
								titleid=null;
							}
						}
						String param = "--titlekey "+titlekeyList.get(titleid.toUpperCase());
						pop("Success! You can use hactool to get your RomFS or exeFS now, but you have to use the titlekey parameter:\n"+param+"\nThis has been copied to your clipboard.");
						setClip(param);
					}

				}
			} else {
				pop("Don't close out of my file chooser! D:<");
			}
		});
		panelDecrypting.add(new JLabel("  "));
		JButton getKeysAgain = new JButton("Get Title Keys Again");
		panelDecrypting.add(getKeysAgain);
		getKeysAgain.addActionListener(ae-> {
			stage=Stage.title_key_save;
			loadCard();
		});



		loadCard();
	}
	private void loadStage() {
		if(!new File(directory+"/biskeys.txt").exists()) return; else loadBiskeys();
		if(!new File(directory+"/sd_seed.txt").exists()) {
			stage=Stage.sd_key_private;
			return;
		} else loadSeed();
		if(!new File(directory+"/eticket.txt").exists()) {
			stage=Stage.title_key_eticket;
			return;
		} else loadEticket();
		if(!new File(directory+"/SDTool/PRODINFO.bin").exists()) {
			stage=Stage.title_key_prod;
			return;
		}
		if(!new File(directory+"/SDTool/title_keys.txt").exists()) {
			stage=Stage.title_key_make_blobs;
			return;
		} else loadTitleKeys();

		stage=Stage.decrypting;
	}
	private void loadCard() {
		CardLayout l = (CardLayout) this.getLayout();
		l.show(this, stage.toString());
		log("Loading stage card "+stage.toString());
		this.revalidate();
	}
	private void preCheck() {
		if(!new File(directory+"/prod.keys").exists()) {
			pop(directory+"/prod.keys does not exist. Please get your keys, then rerun this program.");
			System.exit(1);
		}
		File cur = new File(directory+"/SDTool/payloads/");
		if(!cur.exists())
			if(!cur.mkdirs()) {
				log("SDTool/payloads/ doesn't exist and cannot be created. Exiting...");
				System.exit(1);
			}
		cur = new File(directory+"/SDTool/scripts");
		if(!cur.exists()) {
			if (!cur.mkdir()) {
				log("SDTool/scripts/ doesn't exist and cannot be created. Exiting...");
				System.exit(1);
			}
			try {
				Files.copy(getClass().getResourceAsStream("get_ticketbins.py"), Paths.get(directory+"/SDTool/scripts/get_ticketbins.py"), StandardCopyOption.REPLACE_EXISTING);
				Files.copy(getClass().getResourceAsStream("get_titlekeys.py"), Paths.get(directory+"/SDTool/scripts/get_titlekeys.py"), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				e.printStackTrace();
				pop("Failed to extract scripts. Exiting...");
				System.exit(1);
			}
		}

		if(!new File(directory+"/SDTool/smash.exe").exists()) {
			try {
				Files.copy(getClass().getResourceAsStream("TegraRcmSmash.exe"), Paths.get(directory+"/SDTool/smash.exe"), StandardCopyOption.REPLACE_EXISTING);
				Files.copy(getClass().getResourceAsStream("hactool.exe"), Paths.get(directory+"/SDTool/hactool.exe"), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				e.printStackTrace();
				pop("Failed to extract TegraRcmSmash. Exiting...");
				System.exit(1);
			}
		}
		if(!new File(directory+"/SDTool/payloads/biskey.bin").exists()) {
			try {
				Files.copy(getClass().getResourceAsStream("biskeydump.bin"), Paths.get(directory+"/SDTool/payloads/biskey.bin"), StandardCopyOption.REPLACE_EXISTING);
			} catch(Exception e) {
				e.printStackTrace();
				pop("Failed to extract biskeydump. Exiting...");
				System.exit(1);
			}
		}
		if(!new File(directory+"/SDTool/payloads/memloader.bin").exists()) {
			if(!new File(directory+"/SDTool/payloads/memloader/uboot").mkdirs()) log("failed making directories, but they may already exist.");
			try {
				String m="memloader/", u=m+"uboot/"; /*Apologies for the weirdness, I just wanted to make this smaller*/
				String[] files = {m+"ums_sd.ini", m+"ums_emmc.ini", u+"u-boot.elf", u+"ums_emmc.scr", u+"ums_emmc.scr.img", u+"ums_sd.scr", u+"ums_sd.scr.img"};
				for(String file : files)
					Files.copy(getClass().getResourceAsStream(file), Paths.get(directory+"/SDTool/payloads/"+file), StandardCopyOption.REPLACE_EXISTING);

				Files.copy(getClass().getResourceAsStream("memloader.bin"), Paths.get(directory+"/SDTool/payloads/memloader.bin"), StandardCopyOption.REPLACE_EXISTING);
			} catch(Exception e) {
				e.printStackTrace();
				pop("Failed to extract memloader. Exiting...");
				System.exit(1);
			}
		}
		if(!new File(getDefaultDrive()+":/Python27").exists()) {
			pop("Please install Python 2.7. It is needed for this.");
			try {
				Desktop.getDesktop().browse(URI.create("https://www.python.org/downloads/release/python-2715/"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(1);
		} else {
			log("Python27 is installed, checking for pycrypto...");
			File pycrypto = new File(getDefaultDrive()+":/Python27/Lib/site-packages/Crypto");
			if(pycrypto.exists())
				log("Pycrypto is installed.");
			else
				try {
					log("Pycrypto is not installed, attempting install...");
					Scanner s = new Scanner(Runtime.getRuntime().exec("C:/Python27/Scripts/pip.exe install pycrypto").getInputStream());
					StringBuilder sb = new StringBuilder();
					while(s.hasNext()) {
						sb.append(s.nextLine().replace("\0",""));
						sb.append("\n");
					}
					String output = sb.toString();
					if(output.contains("C++")) {
						log("Failed to install pycrypto.");
						pop("You need Microsoft Visual C++ Compiler for Python 2.7.");
						Desktop.getDesktop().browse(URI.create("https://www.microsoft.com/en-us/download/details.aspx?id=44266"));
						System.exit(1);
					} else {
						try { Thread.sleep(100); } catch (Exception ignored) { }
						if(pycrypto.exists())
							log("Pycrypto installed.");
						else {
							log("Pycrypto failed to install.");
							pop("Failed installing pycrypto, please do it manually.\npip install pycrypto");
							System.exit(1);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}
	private static String getDefaultDrive() {
		if(defaultDrive==null)
			defaultDrive=System.getenv("windir").substring(0,1);
		return defaultDrive;
	}
	private static void setClip(String text) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}
	private static boolean loadSave(String drive) {
		File save = new File(drive + ":/save/8000000000000043");
		if(!save.exists()) {
			pop("Incorrect drive. Please make sure it is your SYSTEM partition.");
			return false;
		}
		StringBuilder builder = new StringBuilder();
		try (FileInputStream s = new FileInputStream(save)) { /*Read it into hex*/
			int length = 0;
			byte dat[] = new byte[16];
			while(length!=-1) {
				length=s.read(dat);
				for(int i=0; i<length; i++) builder.append(String.format("%02x", dat[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			pop("failed :(");
			return false;
		}
		int index=builder.toString().indexOf(clipboard);
		if(index==-1) {
			pop("Something broke, and I don't really know what you did wrong.");
			return false;
		}
		String sdSeed=builder.toString().substring(index+32, index+64).toUpperCase();
		if(!writeToFile(directory+"/sd_seed.txt", sdSeed)) {
			pop("There was an error saving your SD seed:\n"+sdSeed);
			return false;
		}
		SDTool.sdSeed=sdSeed;

		if(!loadTitleSave(drive)) return false;
		pop("Your SD seed has been saved in ./switch.");
		return true;
	}
	private static boolean loadTitleSave(String drive) {
		try {
			Files.copy(Paths.get(drive+":/save/80000000000000e1"), Paths.get(directory+"/SDTool/scripts/80000000000000e1"));
			Files.copy(Paths.get(drive+":/save/80000000000000e2"), Paths.get(directory+"/SDTool/scripts/80000000000000e2"));
		} catch (IOException e) {
			e.printStackTrace();
			pop("Failed. (8)");
			return false;
		}
		return true;
	}
	private static boolean loadPrivate(String drive) {
		File priv = new File(drive + ":/Nintendo/contents/private");
		if(!priv.exists()) {
			pop("SD card letter is incorrect, or you have never used it with your Switch.");
			return false;
		}
		StringBuilder builder = new StringBuilder();
		try (FileInputStream s = new FileInputStream(priv)) { /*Read it into hex*/
			int length = 0;
			byte dat[] = new byte[16];
			while(length!=-1) {
				length=s.read(dat);
				for(int i=0; i<length; i++) builder.append(String.format("%02x", dat[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			pop("failed :(");
			return false;
		}
		File[] fs=new File(drive+":/").listFiles((dir, name) -> name.toLowerCase().endsWith(".ini"));
		if(fs!=null && fs.length!=0) {
			for (File f : fs)
				if (!f.renameTo(new File(f.getName().split(Pattern.quote("."))[0] + ".ini_d")))
					log("Failed renaming " + f.getName());
			pop(".ini files on your SD card has been renamed to be .ini_d for now to make the rest of this process faster.\nThis will be undone at the end for you.");
		}
		clipboard=builder.toString();
		log(clipboard);
		return true;
	}
	private static boolean writeToFile(String file, String toWrite) {
		File f = new File(file);
		if(!f.exists())
			try {
				if(!f.createNewFile()) {
					pop("Failed to create new file.");
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
				pop("Failed to create new file.");
				return false;
			}
		String[] data = toWrite.split("\n");
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for(String s:data) {
				bw.write(s);
				bw.newLine();
			}
		} catch(Exception e) {
			e.printStackTrace();
			pop("Failed to write SD seed.");
			return false;
		}
		return true;
	}
	private static void loadSeed() {
		try {
			sdSeed=Files.readAllLines(Paths.get(directory+"/sd_seed.txt")).get(0);
		} catch (IOException e) {
			e.printStackTrace();
			pop("Failed loading sd seed.");
			System.exit(1);
		}
	}
	private static void loadTitleKeys() {
		try {
			List<String> keys = Files.readAllLines(Paths.get(directory+"/SDTool/title_keys.txt"));
			for(String key : keys) {
				String[] data = key.split(":");
				titlekeyList.put(data[0], data[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static boolean getBiskeys() {
		String keys = smashWithOutput("\""+directory+"/SDTool/payloads/biskey.bin\" -r");
		if(keys.length()<100) {
			pop("Your Switch was not plugged in to your computer/was not in RCM/drivers were not installed. Fix this, and try again.");
			return false;
		}
		keys=keys.substring(keys.indexOf("HWI:"));
		writeToFile(directory+"/biskeys.txt", keys);
		loadBiskeys(keys);
		return true;
	}
	private static void loadBiskeys(String keys) {
		keys=keys.substring(keys.indexOf("BIS KEY"));
		keys=keys.replace("BIS KEY ","").replace(" (","").replace("crypt): ", "c ").replace("tweak): ", "t ");
		for(String key : keys.split("\n")) {
			biskeyList.put(key.substring(0, 2), key.substring(3));
			log("Biskey " + key.substring(0, 2) + " loaded.");
		}
	}
	private void loadBiskeys() {
		StringBuilder keys=new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(directory+"/biskeys.txt"))) {
			String st;
			while ((st = br.readLine()) != null){
				keys.append(st);
				keys.append("\n");
			}
		} catch(Exception e) {
			pop("There was an error loading previously grabbed biskeys. Please get them again.");
			stage = Stage.biskeys;
			loadCard();
		}
		loadBiskeys(keys.toString());
	}
	private void loadEticket() {
		try {
			eticket=Files.readAllLines(Paths.get(directory+"/eticket.txt")).get(0);
		} catch (IOException e) {
			e.printStackTrace();
			pop("Failed to load previously gotten eticket.");
			stage=Stage.title_key_eticket;
		}
	}
	private static String smashWithOutput(String cmd) {
		String output="";
		try {
			Scanner s = new Scanner(Runtime.getRuntime().exec("\"" + directory + "/SDTool/smash.exe\" " + cmd).getInputStream());
			if (!s.hasNext()) {
				pop("Failed running TegraRcmSmash. Please try again. (1)");
				return output;
			}
			StringBuilder build = new StringBuilder();
			while (s.hasNext()) {
				String str = s.nextLine();
				if (str.length() > 2) {
					build.append(str);
					build.append("\n");
				}
			}
			output=build.toString().replace("\0","");
		} catch(Exception e) {
			e.printStackTrace();
			pop("Failed running TegraRcmSmash. Please try again. (2)");
		}
		return output;
	}
	private static boolean smashFailed(String cmd) {
		try {
			int e = Runtime.getRuntime().exec("cmd.exe /c start /min \"\" \"" + directory + "/SDTool/smash.exe\" " + cmd).waitFor();
			log(e+"");
		} catch (Exception e) {
			e.printStackTrace();
			pop("Failed to run TegraRcmSmash. Please try again. (3)");
			return true;
		}
		return false;
	}
	private static String pyscript(String cmd) {
		String output;
		try {
			Scanner s = new Scanner(Runtime.getRuntime().exec(getDefaultDrive()+":/Python27/python.exe "+cmd, null, new File(directory+"/SDTool/scripts/")).getInputStream());
			StringBuilder build = new StringBuilder();
			while (s.hasNext()) {
				String str = s.nextLine();
				if (str.length() > 2) {
					log(str);
					build.append(str);
					build.append("\n");
				}
			}
			output=build.toString().replace("\0","");
		} catch(Exception e) {
			pop("Failed to run the python script. (10)");
			return "";
		}
		log(output);
		return output;
	}
	private static String hactool(String cmd) {
		String output;
		try {
			Scanner s = new Scanner(Runtime.getRuntime().exec("\""+directory+"/SDTool/hactool.exe\" "+cmd, null).getInputStream());
			StringBuilder build = new StringBuilder();
			while (s.hasNext()) {
				String str = s.nextLine().replace("\0","");
				if (str.length() > 2) {
					log(str);
					build.append(str);
					build.append("\n");
				}
			}
			output=build.toString();
		} catch(Exception e) {
			e.printStackTrace();
			pop("Failed to run hactool. (11)");
			return "";
		}
		log(output);
		return output;
	}
}
