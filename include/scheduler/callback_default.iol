type SchedulerCallBackRequest: void {
    .jobName: string
    .groupName: string
}

interface SchedulerCallBackInterface {
OneWay:
  schedulerCallback( SchedulerCallBackRequest )
}

inputPort SchedulerCallBack {
Location:"local"
Interfaces: SchedulerCallBackInterface
}
