# Configure app environment
bs_username = ENV['BROWSER_STACK_USERNAME']
bs_access_key = ENV['BROWSER_STACK_ACCESS_KEY']
bs_local_id = ENV['BROWSER_STACK_LOCAL_IDENTIFIER'] || 'maze_browser_stack_test_id'
bs_device = ENV['DEVICE_TYPE']
app_location = ENV['APP_LOCATION']

# Set this explicitly
$api_key = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"

After do |scenario|
  $driver.reset
end

Before('@skip_android_9') do |scenario|
  skip_this_scenario("Skipping scenario") if bs_device == 'ANDROID_9'
end

Before('@skip_below_android_9') do |scenario|
  skip_this_scenario("Skipping scenario") if bs_device != 'ANDROID_9'
end

Before('@skip_below_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if bs_device != 'ANDROID_8' && bs_device != 'ANDROID_9'
end

Before('@skip_above_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if bs_device == 'ANDROID_8' || bs_device == 'ANDROID_9'
end

AfterConfiguration do |config|
  MazeRunner.configuration.receive_requests_wait = 60
  AppAutomateDriver.new(bs_username, bs_access_key, bs_local_id, bs_device, app_location)
  $driver.start_driver
end

at_exit do
  $driver.driver_quit
end
