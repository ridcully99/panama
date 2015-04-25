# Apache config on debian #

On Debian systems (also Ubuntu) the apache configuration is split into various parts. To achieve the same as described in ApacheConfiguration do something like this:

Enable the required modules

```
a2enmod headers
a2enmod proxy
a2enmod proxy_ajp
a2enmod proxy_http
```

`sudo nano /etc/apache2/sites-available/default`

Add these lines at the end but within `<VirtualHost>` tag.

```
ProxyPass / http://localhost:8080/myapp/
ProxyPassReverse / http://localhost:8080/myapp/
ProxyPassReverseCookiePath /myapp /
```

Restart:

`service apache2 restart`