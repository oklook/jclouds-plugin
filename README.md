# JClouds Plugin for Jenkins

JClouds Jenkins plugin provides option to launch jenkins slaves on any Cloud provider supported by JClouds (http://jclouds.apache.org/reference/providers/). 

### Configuration Options

You can add a new Cloud in the Jenkins configuration. The plugin requires the cloud provider type (as per JClouds Compute API provider list), access id and secret key. You can generate a keypair that will be used to connect and configure the slave.

The list of providers is an auto-complete field. There's an option to test the connection settings, which will validate your credentials. A new node can be launched via Jenkin's computer list (build executors) screen. 

We are working on more configuration options to configure auto-provisioning  and limits etc. 

## Building and Running

You can build and test this plugin using by cloning this repository and 

`mvn clean install hpi:run`

The above command will start jenkins with JClouds plugin pre-configured.

## Adding a new Cloud Provider

* Goto Jenkins Configuration page by clicking on Manage menu or browsing to the URL http://localhost:8080/configure
* Scroll down to Cloud Section
* Click on the `Add a new cloud` pop-up menu button which should have an option - `Cloud (JClouds)`
* Click on `Cloud (JClouds)`
* Fill in the configuration options
  - Profile : the name of the profile e.g, aws-slave-profile
  - Provider Name: type first two characters and you'll get an auto-completed provider name (e.g. aws-ec2 or hpcloud-compute)
  - End Point URL: if your provider API needs an endpoint configuration, add it here, otherwise leave it empty.
  - Max Number of Instances: The maximum number of instances to run from this cloud at one time.
  - Retention Time: How long, in minutes, to wait for a slave to remain idle before disconnecting and terminating it. Defaults to 30.
  - Identity : your accessId
  - Credential: your secret key
  - RSA Private Key/Public Key: If you have a keypair, then just copy paste the public and private key parts, otherwise click on `Generate Key Pair` button.
  - Click on `Test Connection` to validate the cloud settings.
  
* Add Cloud Instance Template by clicking on the Add button
* Fill in configuration options:
  - Name : the name of the instance template e.g. aws-jenkins-slave
  - Number of Executors: How many executors each slave created from this template should have.
  - Description: notes/comments for your reference.
  
  - Image ID: Image ID to use for this slave template, such as EC2 AMIs. Note that EC2 AMIs must include the region as well, e.g., "us-east-1/ami-00000".
  or
  - OSFamily: Specify the OSFamily - leave empty for default for a cloud provider
  - OS Version : Specify the OSVersion - leave empty for default for a cloud provider

  - Hardware ID: Hardware ID on provider for this slave template, such as "t1.micro" on AWS EC2.
  or
  - RAM : in MB
  - No. of Cores: number of virtual processor cores.

  - Location ID: Location ID where this slave will be deployed. If none is selected jclouds will automatically choose an available one.

  - Labels: (space-separated) labels/tags that you can use to attach a build to this slave template
  - Init Script: A shell script to be run when the slave is created.
  - Stop on Terminate: If true, suspend slaves rather than terminating them.

* Click Save to save the configuration changes.
* Goto Jenkins' home page, click on `Build Executor Status` link on the sidebar.
* Verify that you have a button with `Provision via JClouds - {YOUR PROFILE NAME} drop down with the slave template name you configured.
* Click on the slave and see if your slave launched succesfully (please wait until the operation completes).

### Executing build on the slave
* To run your build on the newly configured slave computer, just enable the `Restrict where this project can be run` option in the build configuration page.
* Enter the label which you choose for the instance template in the `Label Expression` text field. This should auto-complete labels for you.
* Click save the save the configuration options.
* Schedule the build to check whether the build is executed on the selected slave template.


## Adding a Blobstore Profile for storing build artifacts

The plugin also provides a way to store your build artifacts on JClouds supported cloud storage providers. You can configure multiple
blobstore profiles and configure the build to copy different files/artifacts to the specified container. Here's how you configure the same.

* Goto Jenkins Configuration Page
* Click Ad under the section `JClouds Cloud Storage Settings`
* Provide the configuration Options:
  - Profile Name: name of the profile e.g. aws-storage
  - Provider Name: [JClouds Supported Provider Name](http://jclouds.apache.org/reference/providers/#blobstore)
  - Identity : your accessId
  - Credential: your secret key
* You can add multiple providers by clicking on Add.
* Click Save on the bottom of the page, to save the settings.

### Publishing artifacts after a build
After you configure a cloudstorage provider, you can enable the publishing file by enabling it under `Post-build Actions` in the build job configuration page.
* Click on the checkbox `Publish artifacts to JClouds Clouds Storage`
* You should now see a dropdown with configured storage profiles. Select the one you want to use for this build.
* Click on Add button next to `Files to upload`.
* Add the sourcd file path (relative to workspace) 
* Add the destination container name.
* Add the virtual path under the container to copy to. (optional)
* Check the Keep Hierarchy box if you want the path of the file relative to the workspace to be appended to the virtual path.
* Repeat to add more files if you want to copy.
* Click save.

When the build is complete and succesful, the configured files will be published to the configured blobstore.

### Jclouds-plugin Extension summary (details in git log) ###
* New Features
  1. Select customized floating IP pool for floating IP association when starting openstack instances.
     - In previous jcloud plugin version, it needs the "default_floating_pool=nova" config in /etc/nova/nova.conf
  2. Give name (based on jenkins env vars BUILD_TAG + BUILD_USER) for openstack instance.
  3. Offline the instance (based on jenkins env var slavePostAction: offline/destroy) and keep in openstack for further debug.
  4. Determine the resource usage in openstack when starting new instance
     - Create some APIs in Jcloud. They are used for resource calculation.
       + getQuotaByTenant
       + getTotalUsageByTenant
       + getFlavorByFlavorId
  5. Give name (existing name + '-offline') for the offline os instance.
  6. Fix the issue in fuzzy search image name by its provided regex function.
     - 1st search the image name input in the regex section of global cloud configuration
     - If no match, use existing fuzzy search to match the regex provided
     - The button "Check Image Name Regex" is enhanced to show the result with above logic
     - The image cache expired time is changed to 5 mins
  7. The instance clean up thread recurrence period is updated from 5 mins to 1 min.
  8. Set the new name in the description of offline OS instance as well as the label is tagged as OfflineOSInstance.
  9. Create a new page $ROOT/jclouds-offline/ for maintaining the OS offline instances.
 10. Add public IP address field in JCloudsSlave for saving it into config.xml. It fixes the issue when jenkins restart,
     the offline slave cannot be re-connected
 11. Enhance the jcloud computer configure page with a offline survive time countdown and a calendar for terminated date selection.
 12. Use the terminated date instead of retention time, for retention audit. Avoid retention time re-calculation issue when jenkins restart.
* Config in jenkins global configuration (just highlight some inportant)
  - Cloud (JClouds)
    + tenantId: get help by clicking "?". Get tenantId by CLI: keystone tenant-list. It's used for getting os quota and usage.
  - Cloud Instance Templates
    + Number of Executors: set to 1 to ensure only 1 executor running on one os instance
  - Image/OS Options
    + Specify Image Name Regex: give the full image name in OS
  - "Use Pre-existing Jenkins User" and "Use Pre-installed Java": get it clicked
  - Open Stack Options
    + "Floating IP Pool": the pool name of floating IPs in OS
    + "Network Range for Floating IP Associated with": give your floating IP range for attached
* Config in each job
  - Label Expression: input your cloud instance template label
  - click "JClouds Single Slave Plugin-Ex" to enable jclouds-plugin for this job
  - jenkins env var $JENKINS_NODE_NAME can be used to get the name of your instance

