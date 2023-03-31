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
import brut.androlib.err.AXmlDecodingException;
import brut.androlib.err.RawXmlEncounteredException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtXmlSerializer;
import brut.androlib.tinypace.AppInfo;
import brut.androlib.tinypace.Global;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.wrapper.XmlPullParserWrapper;
import org.xmlpull.v1.wrapper.XmlPullWrapperFactory;
import org.xmlpull.v1.wrapper.XmlSerializerWrapper;
import org.xmlpull.v1.wrapper.classic.StaticXmlSerializerWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class XmlPullStreamDecoder implements ResStreamDecoder {
    private final static Logger LOGGER = Logger.getLogger(XmlPullStreamDecoder.class.getName());

    public XmlPullStreamDecoder(XmlPullParser parser,
                                ExtXmlSerializer serializer) {
        this.mParser = parser;
        this.mSerial = serializer;
    }

    @Override
    public void decode(InputStream in, OutputStream out)
        throws AndrolibException {
        try {
            XmlPullWrapperFactory factory = XmlPullWrapperFactory.newInstance();
            XmlPullParserWrapper par = factory.newPullParserWrapper(mParser);
            final ResTable resTable = ((AXmlResourceParser) mParser).getAttrDecoder().getCurrentPackage().getResTable();

            XmlSerializerWrapper ser = new StaticXmlSerializerWrapper(mSerial, factory) {
                boolean hideSdkInfo = false;
                boolean hidePackageInfo = false;

                @Override
                public void event(XmlPullParser pp)
                    throws XmlPullParserException, IOException {
                    int type = pp.getEventType();

                    if (type == XmlPullParser.START_TAG) {
                        if ("manifest".equalsIgnoreCase(pp.getName())) {
                            try {
                                hidePackageInfo = parseManifest(pp);
                            } catch (AndrolibException ignored) {
                            }
                        } else if ("uses-sdk".equalsIgnoreCase(pp.getName())) {
                            try {
                                hideSdkInfo = parseAttr(pp);
                                if (hideSdkInfo) {
                                    return;
                                }
                            } catch (AndrolibException ignored) {
                            }
                        }
                    } else if (hideSdkInfo && type == XmlPullParser.END_TAG
                        && "uses-sdk".equalsIgnoreCase(pp.getName())) {
                        return;
                    } else if (hidePackageInfo && type == XmlPullParser.END_TAG
                        && "manifest".equalsIgnoreCase(pp.getName())) {
                        super.event(pp);
                        return;
                    }
                    super.event(pp);
                }

                private boolean parseManifest(XmlPullParser pp)
                    throws AndrolibException {
                    String attr_name;

                    // read <manifest> for package:
                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                        attr_name = pp.getAttributeName(i);

                        if (attr_name.equalsIgnoreCase(("package"))) {
                            resTable.setPackageRenamed(pp.getAttributeValue(i));
                        } else if (attr_name.equalsIgnoreCase("versionCode")) {
                            resTable.setVersionCode(pp.getAttributeValue(i));
                        } else if (attr_name.equalsIgnoreCase("versionName")) {
                            resTable.setVersionName(pp.getAttributeValue(i));
                        }
                    }
                    return true;
                }

                private boolean parseAttr(XmlPullParser pp)
                    throws AndrolibException {
                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                        final String a_ns = "http://schemas.android.com/apk/res/android";
                        String ns = pp.getAttributeNamespace(i);

                        if (a_ns.equalsIgnoreCase(ns)) {
                            String name = pp.getAttributeName(i);
                            String value = pp.getAttributeValue(i);
                            if (name != null && value != null) {
                                if (name.equalsIgnoreCase("minSdkVersion")
                                    || name.equalsIgnoreCase("targetSdkVersion")
                                    || name.equalsIgnoreCase("maxSdkVersion")
                                    || name.equalsIgnoreCase("compileSdkVersion")) {
                                    resTable.addSdkInfo(name, value);
                                } else {
                                    resTable.clearSdkInfo();
                                    return false; // Found unknown flags
                                }
                            }
                        } else {
                            resTable.clearSdkInfo();

                            if (i >= pp.getAttributeCount()) {
                                return false; // Found unknown flags
                            }
                        }
                    }

                    return !resTable.getAnalysisMode();
                }
            };

            par.setInput(in, null);
            ser.setOutput(out, null);

            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                ser.event(par);
            }
            ser.flush();
        } catch (XmlPullParserException ex) {
            throw new AXmlDecodingException("Could not decode XML", ex);
        } catch (IOException ex) {
            throw new RawXmlEncounteredException("Could not decode XML", ex);
        }
    }

    @Override
    public void decodeApplication(InputStream in, OutputStream out, AppInfo appInfo)
        throws AndrolibException {
        try {
            XmlPullWrapperFactory factory = XmlPullWrapperFactory.newInstance();
            XmlPullParserWrapper par = factory.newPullParserWrapper(mParser);
            XmlSerializerWrapper ser = new StaticXmlSerializerWrapper(mSerial, factory) {
                boolean isActivityFlag, isActionMain, isCategoryLauncher;

                @Override
                public void event(XmlPullParser pp)
                    throws XmlPullParserException, IOException {
                    int type = pp.getEventType();

                    if (type == XmlPullParser.START_TAG) {

                        switch (pp.getName()) {
                            case "manifest":
                                for (int i = 0; i < pp.getAttributeCount(); i++) {

                                    switch (pp.getAttributeName(i)) {
                                        case "versionCode":
                                            appInfo.setVersionCode(pp.getAttributeValue(i));
                                            break;
                                        case "versionName":
                                            appInfo.setVersionName(pp.getAttributeValue(i));
                                            break;
                                        case "package":
                                            appInfo.setPackageName(pp.getAttributeValue(i));
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                break;
                            case "uses-sdk":
                                for (int i = 0; i < pp.getAttributeCount(); i++) {
                                    if ("minSdkVersion".equals(pp.getAttributeName(i))) {
                                        appInfo.setSdkVersion(pp.getAttributeValue(i));
                                        break;
                                    }
                                }
                                break;
                            case "application":
                                for (int i = 0; i < pp.getAttributeCount(); i++) {
                                    if ("icon".equals(pp.getAttributeName(i))) {
                                        String[] ss = pp.getAttributeValue(i).split("/");
                                        Global.ICON_NAME = ss[ss.length - 1];
//                                        LOGGER.info("ICON_NAME0:" + Global.ICON_NAME);
                                        break;
                                    } else if ("label".equals(pp.getAttributeName(i))) {
                                        Global.APP_NAME = pp.getAttributeValue(i);
                                    }
                                }
                                break;
                            case "activity":
                                if (isActionMain && isCategoryLauncher) {
                                    break;
                                } else {
                                    isActionMain = false;
                                    isCategoryLauncher = false;
                                }

                                for (int i = 0; i < pp.getAttributeCount(); i++) {
                                    if ("name".equals(pp.getAttributeName(i))) {
                                        appInfo.setLaunchActivity(pp.getAttributeValue(i));
                                        break;
                                    }
                                }
                                isActivityFlag = true;
                                break;
                            case "action":
                                if (!isActionMain && isActivityFlag) {
                                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                                        if ("name".equals(pp.getAttributeName(i))) {
                                            String name = pp.getAttributeValue(i);
                                            if ("android.intent.action.MAIN".equals(name)) {
                                                isActionMain = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            case "category":
                                if (!isCategoryLauncher && isActivityFlag) {
                                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                                        if ("name".equals(pp.getAttributeName(i))) {
                                            String name = pp.getAttributeValue(i);
                                            if ("android.intent.category.LAUNCHER".equals(name)) {
                                                isCategoryLauncher = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                            default:
                                break;
                        }

                    }
                    super.event(pp);
                }

            };

            par.setInput(in, null);
            ser.setOutput(out, null);

            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                ser.event(par);
            }
            ser.flush();
        } catch (XmlPullParserException ex) {
            throw new AXmlDecodingException("Could not decode XML", ex);
        } catch (IOException ex) {
            throw new RawXmlEncounteredException("Could not decode XML", ex);
        }
    }

    public void decodeManifest(InputStream in, OutputStream out)
        throws AndrolibException {
        decode(in, out);
    }

    public void decodeManifestApplication(InputStream in, OutputStream out, AppInfo appInfo)
        throws AndrolibException {
        decodeApplication(in, out, appInfo);
    }

    private final XmlPullParser mParser;
    private final ExtXmlSerializer mSerial;
}
