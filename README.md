# Encrypted SMS Backup

This is an Android app that allows you securely export your SMS conversations to text files.

[Download on Google Play][play-link]

You can't trust other SMS backup programs with Internet access, right? You don't know where they are uploading your private SMS conversations. All of the SMS backup programs on the play store require Internet access. This one doesn't.

UPDATE: Since 2014, all apps are de-facto given INTERNET Access even the app doesn't ask for it. If you're paranoid, you can compile the source code.

If you care about your privacy, this program is what you need. 

This app just reads SMSes and save them as encrypted TXT files on your phone storage, it DOES NOT SEND them anywhere. You can manually move the backup files to your PC/anywhere you want and decrypt there.

Features:

 - Writing each conversation to a different TXT file.
 - Encryption (optional) via jasypt 1.9.1 library.
 - Zipping all conversations (optional)

Example in the file manager:

	EncryptedSMSBackup/2013-10-05-2353/John Doe.txt
	EncryptedSMSBackup/2013-10-05-2353/Jack Brown.txt

with contents:

	[2013-10-02 13:49] John: Hey how are you?
	[2013-10-02 13:50] Me: Fine, and you?
	[2013-10-02 13:51] John: Excellent!

If you encrypt your SMS files, you can decrypt them with SMS-decrypter, a desktop GUI: https://github.com/aladagemre/sms-decrypter/releases

## Licence

This app is licenced with Apache 2 licence. 
Jaspyt library (which is used) is licenced with Apache2 as well. 
Thanks to @Belozerow for his support in initial work.

[play-link]: https://play.google.com/store/apps/details?id=com.aladagemre.SecureSMSBackupPro
