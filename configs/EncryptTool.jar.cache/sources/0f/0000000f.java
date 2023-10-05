package com.ivy.tools;

import java.io.File;

/* loaded from: EncryptTool.jar:com/ivy/tools/EncryptTool.class */
public class EncryptTool {
    public static void main(String[] args) {
        try {
            String path = args[1];
            String outpath = args[2];
            if (!new File(path).exists()) {
                System.out.println("Source file < " + path + " > doesn't exists!");
                return;
            }
            int start = 0;
            int end = 0;
            if (args.length > 3) {
                start = Integer.parseInt(args[3]);
            }
            if (args.length > 4) {
                end = Integer.parseInt(args[4]);
            }
            if (args[0].toLowerCase().equals("e")) {
                CommonUtil.encrypt(new File(path), new File(outpath), start, end);
            } else if (args[0].toLowerCase().equals("ef")) {
                File src = new File(path);
                CommonUtil.encrypt(src, new File(outpath), start, end);
                src.delete();
            } else if (args[0].toLowerCase().equals("d")) {
                CommonUtil.decrypt(new File(path), new File(outpath));
            } else if (args[0].toLowerCase().equals("df")) {
                File src2 = new File(path);
                CommonUtil.decrypt(src2, new File(outpath));
                src2.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}