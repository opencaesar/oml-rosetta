# OML Rosetta Docker container

## Building

The Dockerfile uses a base image from hub.docker.com, which is subject to download rate limits (see: https://docs.docker.com/docker-hub/download-rate-limit/).

If the docker hub container registry is cached, e.g. https://mycompany.com:10000/, then build this image as follow:

```
docker build --build-arg "image_cache=mycompany.com:10000/" -t opencaesar/omlrosetta:latest .
```

Alternatively, use the hub.docker.com registry directly like this:

```
docker build -t opencaesar/omlrosetta:latest .
```

## Running

```
docker run --rm -it -p 8080:8080 opencaesar/omlrosetta:latest
```

Then open a web browser at: http://localhost:8080/vnc-auto.html

The web page will show the virtual X11 display, which will have two X11 client windows:
- Xterm
- the Eclipse-based OML Rosetta GUI application

## Limitations

- The container uses supervisord instead of systemctl; installing firefox seems to require systemctl and snap.
- There is no support for copy/paste between the host and the container.