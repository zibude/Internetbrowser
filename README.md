# Internetbrowser
This repository allows you to view the latest websites to some extent on devices that do not support TLS, such as the HTC Magic.

# Operating principle
In this state, the app and server process the request sent by the app using the latest TLS on the server, reformat it into a format that older devices like the HTC Magic can understand, and then send it to the server for display.
It is done seamlessly, and you can expect sufficiently fast speeds even when taking into account the processing power of the device and the operation of a proxy server.
Of course, images are also displayed via the server and converted to a size and number of colors suitable for smartphones.

# Understanding of images and CSS
This depends on the site, but relatively easy-to-understand sites like Wikipedia and the mobile version of Google can be viewed without any layout issues. Modern sites like YouTube and those that make extensive use of the latest CSS may not be viewable, or may open but be slow or have layout issues. This needs improvement.

#Features and Operation
The functionality is very simple 
・Back, forward
・Reload
・Tabs
Press the main unit menu button
・Find page
・Bookmark
・IP address change
These exist

# Operating conditions
・A PC (or smartphone) capable of running Python ・A PC capable of running ADB ・An Android 1.X–2.X smartphone (We have confirmed that it works on Android 15, but we do not recommend it.)