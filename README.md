# GPDroid-NFC -- Global Platform Card Management Tool using NFC-API for Android

This Android application is designed to manage applets on GlobalPlatform-compliant
contactless smartcards. You can install and delete applets to and from a contactless
smartcard over NFC. Additionally, you may list all installed applets on a contactless
smartcard. This tool is only made for development and testing. It is not recommended
to use it in bulk deployment.



## DISCLAIMER

Your are using this application at your own risk. Incorrect usage may cause hardware
damages and lead to permanently broken devices. It is essential to have profound
knowledge of smartcard GlobalPlatform, APDU-communication and the theoretical
background. Furthermore, you have to know the appropriate keys and secure channel
parameters for your smartcard.

*We are not responsible for any damage caused by this application, incorrect usage
or correctness of this manual.*



## REQUIREMENTS

First of all, to use the application you need knowledge about smartcards and
GlobalPlatform card management.

The application itself uses the NFC-API to communicate with contactless smartcards.



## INSTALLATION

This application is designed for Android 4.1+. Additionally, you need a contactless
smartcard for which you have the appropriate keys for management of a security domain.
The application has been tested with YubiKey NEO.

You can build and install the application like any other Android application with
any development IDE of your choice. If you want to use Eclipse, you can simply import
the package.



## USER GUIDE


### 1. Initialization

Before you can start with managing applets, you may have to scan the contactless
smartcard with your Android device and select "NFC Interface" from the list of
available readers. After you selected the reader, it is necessary to enter the
correct keyset for your smartcard. (NOTE: GPDroid-NFC currently only supports
access to the ISD on most smartcards and does not support management of
supplementary security domains.) Furthermore, the parameters for the secure channel
have to be set in a similar way. Default parameters are preinstalled, which may not
necessarily fit for your smartcard.


### 2. Keyset and Secure Channel Parameters


#### 2.1. Setting the Keyset

For your custom keyset, you begin by selecting a name, which you want to see in the
selection. You may freely choose this name to your needs. This keyset will be bound
to the selected reader! The ID has to match the keyset ID on the smartcard. If you
don’t know the ID, look into the documentation of the smartcard. You may find it
there together with the appropriate keys. The ID value is between 0 and 255.

Afterwards you have to select a version number for the keyset and you have to set
the 3 HEX-encoded keys for your smartcard. If you don’t know them, it is strongly
recommended to stop using the application. You may brick your smartcard by
authenticating with wrong keys, keyset version or keyset ID.

After setting all parameters, click OK and the data will be saved to your local
storage and will be available in the spinner. Note that you have to set the ID as
well as the version before you can continue.


#### 2.2. Setting Secure Channel Paramters

For the secure channel, you have to set 3 parameters. First, you may freely choose
a name for the channel. The first parameter is the SCP version. The second parameter
is the security level of the secure channel. Please note, that this may only be a
value between 0 and 255. As a last parameter tick the checkbox if your card is a
Gemalto smartcard. To finish and complete the settings page, click OK. Note, that
the version as well as the security level has to be set.


### 3. Managing Applets

Pressing the "Choose Applet" button will prompt you with a selector for a
file-manager. If you have not yet installed any file-manager, please check Google's
Play Store (or your preferred application store) to get one. In the file manager,
select the applet that you want to install on the card. The selected applet will be
shown in the text box above the buttons.

*Before you install the applet, please check your previously set parameters for
correctness!*

After clicking the "Install Applet" button, you will be prompted with a screen
asking you for further install parameters. If no parameters are necessary, please
enter nothing or 0. Only if you are sure of the appropriate parameters, please type
them into the editing fields. As soon as you click "Set", the install process will
be started. If an error occurs, it will be shown in the log area. If some sort of
crypto error or exception occurs, your keys or parameters may be wrong and you
should double-check them!

*Continuing with wrong keys may permanently brick your smartcard!*

Clicking the "List Applets" button will give you a list of installed load files
(application packages) and security domains on the card.

Clicking “Get Data” will show you another screen, where you can enter the two
parameters P1 and P2 for to issue a GET DATA APDU command. The response will be
shown in the log area as a string of hexadecimal digits.
