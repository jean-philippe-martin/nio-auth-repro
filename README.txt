Does authentication work correctly when we're using both NIO and Dataproc? This test aims to find
out (for this particular configuration at least).

Steps to follow:

(1) Create a Dataproc cluster

This is done via http://cloud.google.com/console
Give it two workers; it's OK to use puny machines with a small amount of storage.
Make a note of which region you picked.
Wait for the cluster to be up and running.

PROJECT=(type here the project you used for the cluster)
REGION=(type here the region you picked for the cluster)
CLUSTER=(type here the name you picked for the cluster)

for example:

PROJECT=my-test-project
REGION=us-west1
CLUSTER=jps-test-cluster

(2) Pick a file to use

// replace this with a file you control.
BUCKET=jpmartin-testing-project
TESTPATH='/hellbender-test-inputs/CEUTrio.HiSeq.WGS.b37.ch20.1m-2m.NA12878.bam'

// Check that the file exists. This should work.
gsutil ls -lh "gs://${BUCKET}${TESTPATH}"

(2) Set up gcloud's default credentials

gcloud config set project $PROJECT
gcloud auth application-default login

(3) Compile the code

gradle sparkJar

(4) Run the code locally, making sure it works in non-Dataproc conditions.

java -jar build/libs/nio-auth-repro-package-1.0-spark.jar ${BUCKET} ${TESTPATH} --local

As of this writing, part (4) works as expected.

(5) Run the Dataproc test

gcloud dataproc jobs submit spark \
  --project $PROJECT \
  --region $REGION \
  --cluster $CLUSTER \
  --jar build/libs/nio-auth-repro-package-1.0-spark.jar \
  -- ${BUCKET} ${TESTPATH} --sparkMaster yarn

It'll say:
Job [69e0fbee-8d8d-46b2-a038-438c6b1044e3] submitted.

If necessary, kill it with:

gcloud dataproc jobs kill  69e0fbee-8d8d-46b2-a038-438c6b1044e3

(matching the job ID given earlier)

The test should report success, but as of this writing it doesn't, neither for the local
nor the remote part.

Reference:
https://github.com/GoogleCloudPlatform/google-cloud-java/issues/2453