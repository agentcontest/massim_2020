# MASSim Docker files

These may give you an idea of how to run the MASSim server and some of the agent platforms.

To run the configurations (install Docker first, then):

- Navigate to `docker-compose.yml` and comment (out) the parts that you (don't) want to run.
- Follow the special instructions below for the chosen platform(s).
- `docker-compose build`
- `docker-compose up`


## Special instructions

### For EIS-enabled agent platforms

- Build MASSim locally or download a release.
  - Copy `eismassim/target/eismassim-*-jar-with-dependencies.jar` to `lib/eismassim.jar`, and
  - copy an `eismassimconfig.json` to `lib/eismassimconfig.json`.
  - These will be mounted where necessary.


## Notes

- You can configure the Java version and the URLs of the agent platform executables in the `.env` file.