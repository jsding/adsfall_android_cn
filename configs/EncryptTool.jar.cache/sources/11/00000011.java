package com.ivy.tools;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* loaded from: EncryptTool.jar:com/ivy/tools/FixPublicId.class */
public class FixPublicId {
    static final int BUFFER = 2048;
    static final ArrayList<File> Rfs = new ArrayList<>();
    static final ArrayList<String> Rfsd = new ArrayList<>();
    static final HashMap<File, String> others = new HashMap<>();
    static final HashMap<String, Boolean> hasModified = new HashMap<>();

    public static void main(String[] args) {
        fixPublicId(args[0]);
    }

    public static void fixPublicId(String path) {
        String s;
        int start;
        try {
            String path2 = CommonUtil.fixPath(path + "/");
            getFiles(new File(path2 + "smali"));
            File file = new File(path2 + "res/values/public.xml");
            if (file.exists()) {
                FileInputStream fin = new FileInputStream(file);
                byte[] b = new byte[(int) file.length()];
                fin.read(b);
                fin.close();
                String data = new String(b, "UTF-8");
                String[] tmp = data.split("<public");
                HashMap<File, String> rf = new HashMap<>();
                for (String str : tmp) {
                    int idx = str.indexOf("name=\"");
                    if (idx >= 0) {
                        int idx2 = idx + 6;
                        String name = str.substring(idx2, str.indexOf("\" ", idx2)).replaceAll("\\.", "_");
                        int idx3 = str.indexOf("id=\"");
                        if (idx3 >= 0) {
                            int idx4 = idx3 + 4;
                            String id = str.substring(idx4, str.indexOf("\" ", idx4));
                            int idx5 = str.indexOf("type=\"");
                            if (idx5 >= 0) {
                                int idx6 = idx5 + 6;
                                String type = str.substring(idx6, str.indexOf("\" ", idx6)) + ".smali";
                                int n = Rfsd.size();
                                for (int i = 0; i < n; i++) {
                                    File f = Rfs.get(i);
                                    if (f.toString().contains(type) && (start = (s = Rfsd.get(i)).indexOf("final " + name + ":I")) >= 0) {
                                        int start2 = start + name.length() + 11;
                                        int end = start2 + 10;
                                        String r = s.substring(start2, end);
                                        if (!id.equals(r)) {
                                            System.out.println("[Replace File] " + f.toString());
                                            System.out.println("[Replace] " + name + " : " + r + " : " + id);
                                            String s2 = s.replace("final " + name + ":I = " + r, "final " + name + ":I = " + id);
                                            Rfsd.set(i, s2);
                                            rf.put(f, s2);
                                            Set<Map.Entry<File, String>> entrySet = others.entrySet();
                                            for (Map.Entry<File, String> entry : entrySet) {
                                                String value = entry.getValue();
                                                File other = entry.getKey();
                                                String otherPath = other.toString();
                                                if (value.contains(" " + r)) {
                                                    String[] arr = value.split("\n");
                                                    for (int j = 0; j < arr.length; j++) {
                                                        if (!hasModified.containsKey(CommonUtil.md5(otherPath + arr[j])) && arr[j].contains(" " + r)) {
                                                            arr[j] = arr[j].replaceAll(" " + r, " " + id);
                                                            if (arr[j].contains("const/high16")) {
                                                                arr[j] = arr[j].replaceAll("const/high16", "const");
                                                            }
                                                            hasModified.put(CommonUtil.md5(otherPath + arr[j]), true);
                                                        }
                                                    }
                                                    String value2 = CommonUtil.join("\n", arr);
                                                    others.put(other, value2);
                                                    rf.put(other, value2);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Set<Map.Entry<File, String>> entrySet2 = rf.entrySet();
                for (Map.Entry<File, String> entry2 : entrySet2) {
                    CommonUtil.writeFile(entry2.getKey(), entry2.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getFiles(File file) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file2 : files) {
                getFiles(file2);
            }
            return;
        }
        String path = file.toString();
        if (path.contains("R$")) {
            Rfs.add(file);
            Rfsd.add(CommonUtil.readFile(file, ""));
            return;
        }
        others.put(file, CommonUtil.readFile(file, ""));
    }
}