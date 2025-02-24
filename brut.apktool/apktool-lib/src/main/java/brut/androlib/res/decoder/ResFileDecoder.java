/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFind9PatchChunkException;
import brut.androlib.err.RawXmlEncounteredException;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.value.ResBoolValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.tinypace.AppInfo;
import brut.androlib.tinypace.Global;
import brut.directory.DirUtil;
import brut.directory.Directory;
import brut.directory.DirectoryException;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResFileDecoder {
    private final ResStreamDecoderContainer mDecoders;

    public ResFileDecoder(ResStreamDecoderContainer decoders) {
        this.mDecoders = decoders;
    }

    public void decode(ResResource res, Directory inDir, Directory outDir, Map<String, String> resFileMapping) throws AndrolibException {

        ResFileValue fileValue = (ResFileValue) res.getValue();
        String inFilePath = fileValue.toString();
        String inFileName = fileValue.getStrippedPath();
        String outResName = res.getFilePath();
        String typeName = res.getResSpec().getType().getName();

        String ext = null;
        String outFileName;
        int extPos = inFileName.lastIndexOf(".");
        if (extPos == -1) {
            outFileName = outResName;
        } else {
            ext = inFileName.substring(extPos).toLowerCase();
            outFileName = outResName + ext;
        }

        String outFilePath = "res/" + outFileName;
        if (!inFilePath.equals(outFilePath)) {
            resFileMapping.put(inFilePath, outFilePath);
        }

        try {
            if (typeName.equals("raw")) {
                decode(inDir, inFilePath, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("font") && !".xml".equals(ext)) {
                decode(inDir, inFilePath, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("drawable") || typeName.equals("mipmap")) {
                if (inFileName.toLowerCase().endsWith(".9" + ext)) {
                    outFileName = outResName + ".9" + ext;

                    // check for htc .r.9.png
                    if (inFileName.toLowerCase().endsWith(".r.9" + ext)) {
                        outFileName = outResName + ".r.9" + ext;
                    }

                    // check for raw 9patch images
                    for (String extension : RAW_9PATCH_IMAGE_EXTENSIONS) {
                        if (inFileName.toLowerCase().endsWith("." + extension)) {
                            copyRaw(inDir, outDir, inFilePath, outFileName);
                            return;
                        }
                    }

                    // check for xml 9 patches which are just xml files
                    if (inFileName.toLowerCase().endsWith(".xml")) {
                        decode(inDir, inFilePath, outDir, outFileName, "xml");
                        return;
                    }

                    try {
                        decode(inDir, inFilePath, outDir, outFileName, "9patch");
                        return;
                    } catch (CantFind9PatchChunkException ex) {
                        LOGGER.log(Level.WARNING, String.format("Cant find 9patch chunk in file: \"%s\". Renaming it to *.png.", inFileName), ex);
                        outDir.removeFile(outFileName);
                        outFileName = outResName + ext;
                    }
                }

                // check for raw image
                for (String extension : RAW_IMAGE_EXTENSIONS) {
                    if (inFileName.toLowerCase().endsWith("." + extension)) {
                        copyRaw(inDir, outDir, inFilePath, outFileName);
                        return;
                    }
                }

                if (!".xml".equals(ext)) {
                    decode(inDir, inFilePath, outDir, outFileName, "raw");
                    return;
                }
            }

            decode(inDir, inFilePath, outDir, outFileName, "xml");
        } catch (RawXmlEncounteredException ex) {
            // If we got an error to decode XML, lets assume the file is in raw format.
            // This is a large assumption, that might increase runtime, but will save us for situations where
            // XSD files are AXML`d on aapt1, but left in plaintext in aapt2.
            decode(inDir, inFilePath, outDir, outFileName, "raw");
        } catch (AndrolibException ex) {
            LOGGER.log(Level.SEVERE, String.format("Could not decode file, replacing by FALSE value: %s", inFileName), ex);
            res.replace(new ResBoolValue(false, 0, null));
        }
    }

    public void decodeArsc(ResResource res, Directory inDir, Directory outDir, Map<String, String> resFileMapping) throws AndrolibException {

        ResFileValue fileValue = (ResFileValue) res.getValue();
        String inFilePath = fileValue.toString();
        String inFileName = fileValue.getStrippedPath();


        String outResName = res.getFilePath();
        String typeName = res.getResSpec().getType().getName();

        String ext = null;
        String outFileName;
        int extPos = inFileName.lastIndexOf(".");
        if (extPos == -1) {
            outFileName = outResName;
        } else {
            ext = inFileName.substring(extPos).toLowerCase();
            outFileName = outResName + ext;
        }

        String outFilePath = "res/" + outFileName;
        if (!inFilePath.equals(outFilePath)) {
            resFileMapping.put(inFilePath, outFilePath);
        }

        String fileName = inFileName;
        if (inFileName.contains("/")) {
            String[] ss = inFileName.split("/");
            fileName = ss[ss.length - 1];
        }
        if (fileName.equals(Global.ICON_NAME)) {
            Global.ICON_NAME = outResName.split("/")[1];
        }

//        LOGGER.info("inFilePath:" + inFilePath);
//        LOGGER.info("inFileName:" + inFileName);
//        LOGGER.info("outResName:" + outResName);
//        LOGGER.info("typeName:" + typeName);
        try {
//            if ("drawable".equals(typeName) || "mipmap".equals(typeName)) {
            if (typeName.contains("drawable") || typeName.contains("mipmap")) {
                if (!".xml".equals(ext)) {
                    String name = outResName.split("/")[1];
                    if (name.equals(Global.ICON_NAME)) {
                        LOGGER.info("outFileName:" + outFileName);
                        // 判断输出图片后缀，没有则加上
                        if (!outFileName.contains(".")) {
                            outFileName += ".png";
                        }
                        decode(inDir, inFilePath, outDir, outFileName, "raw");
                    }
                }
            } else if ("string".equals(typeName)) {
                decode(inDir, inFilePath, outDir, outFileName, "xml");
            }
        } catch (RawXmlEncounteredException ex) {
            // If we got an error to decode XML, lets assume the file is in raw format.
            // This is a large assumption, that might increase runtime, but will save us for situations where
            // XSD files are AXML`d on aapt1, but left in plaintext in aapt2.
//            LOGGER.info("RawXmlEncounteredException");
            decode(inDir, inFilePath, outDir, outFileName, "raw");
        } catch (AndrolibException ex) {
//            LOGGER.log(Level.SEVERE, String.format(
//                "Could not decode file, replacing by FALSE value: %s",
//                inFileName), ex);
            res.replace(new ResBoolValue(false, 0, null));
        }
    }

    public void decode(Directory inDir, String inFileName, Directory outDir, String outFileName, String decoder) throws AndrolibException {
        try (InputStream in = inDir.getFileInput(inFileName); OutputStream out = outDir.getFileOutput(outFileName)) {
            mDecoders.decode(in, out, decoder);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void copyRaw(Directory inDir, Directory outDir, String inFilename, String outFilename) throws AndrolibException {
        try {
            DirUtil.copyToDir(inDir, outDir, inFilename, outFilename);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifest(Directory inDir, String inFileName, Directory outDir, String outFileName) throws AndrolibException {
        try (InputStream in = inDir.getFileInput(inFileName); OutputStream out = outDir.getFileOutput(outFileName)) {
            ((XmlPullStreamDecoder) mDecoders.getDecoder("xml")).decodeManifest(in, out);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifestApplication(Directory inDir, String inFileName, Directory outDir, String outFileName, AppInfo appInfo) throws AndrolibException {
        try (InputStream in = inDir.getFileInput(inFileName); OutputStream out = outDir.getFileOutput(outFileName);) {
            ((XmlPullStreamDecoder) mDecoders.getDecoder("xml")).decodeManifestApplication(in, out, appInfo);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private final static Logger LOGGER = Logger.getLogger(ResFileDecoder.class.getName());

    private final static String[] RAW_IMAGE_EXTENSIONS = new String[]{"m4a", // apple
        "qmg", // samsung
    };

    private final static String[] RAW_9PATCH_IMAGE_EXTENSIONS = new String[]{"qmg", // samsung
        "spi", // samsung
    };
}
