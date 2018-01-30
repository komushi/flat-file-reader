FROM anapsix/alpine-java:8
ADD target/flat-file-reader-0.1.0-jar-with-dependencies.jar /main/flat-file-reader.jar
ADD data/sample_data_tokyo_23.csv /data/sample_data_tokyo_23.csv
VOLUME ["/tmp"]
ENTRYPOINT ["java","-jar","/main/flat-file-reader.jar","--file=./data/sample_data_tokyo_23.csv","--mode=bulk","--interval=100"]