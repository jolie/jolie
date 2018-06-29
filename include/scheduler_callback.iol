type SchedulerCallBackRequest: void {
    .jobName: string
    .groupName: string
}

interface SchedulerCallBackInterface {
  OneWay:
    __scheduler_callback( SchedulerCallBackRequest )
}

inputPort SchedulerCallBack {
  Location:"local"
  Interfaces: SchedulerCallBackInterface
}
