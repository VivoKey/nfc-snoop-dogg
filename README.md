NFC-SNOOP-DOGG
==============

<img src="doggy.png" width="151" height="199" align="right" />

Live monitoring of Android `NFCSNOOP` packets via USB or wireless debugging.

Captures low-level NFC controller interface (NCI) data. Root not required.

Make sure to enable unfiltered NFC logging in developer options.

Thanks to [@rileyg98](https://github.com/VivoKey/NFCSnoopDecoder) and [@snake-4](https://github.com/snake-4/NFC-NCI-Decoder) for making this possible!

This project is free and open-source software.

Installation
------------

First you must enable NFC NCI unfiltered log in Developer Options.

![](settings.png)

Now install the APK using adb.

```
$ adb install nfc-snoop-dogg.apk
```

Usage
-----

On Linux systems execute the following command to launch:
```
adb shell '$(content read --uri content://nfcsnoop)'
```

On Windows systems you must execute the command using double quotes:
```
adb shell "$(content read --uri content://nfcsnoop)"
```



