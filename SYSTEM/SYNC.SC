;;;;
;;;;  SYNC.SC
;;;;
;;;;  (c) Sierra On-Line, Inc, 1992
;;;;
;;;;  Author:  Unknown
;;;;  Updated: Brian K. Hughes
;;;;
;;;;  Create a hook between talker & mouthSync to that a mouth can be
;;;;  assigned a specific channel (1 through 4) which will cause a different
;;;;  masking word to be used in Sync
;;;;
;;;;  Classes:
;;;;     Sync
;;;;     ScriptSync
;;;;     MouthSync


(script# SYNC)


(class Sync kindof Object
   (properties
      syncTime -1
      syncCue  -1
      prevCue  -1
      syncNum  -1
   )
   (methods
      syncStart
      syncCheck
      syncStop
   )
   
   (method (syncStart modnum noun verb case seq)
      (DoSync StartSync self modnum noun verb case seq)
      (if (!= syncCue -1)
         (= prevCue syncCue)
         (= syncTime 0)
      )
   )

   (method (syncCheck)
      (if (!= syncCue -1)
         (if (or  (u<= syncTime syncBias)
                  (<= syncTime (DoAudio Loc))
            )
            (if (== (& $fff0 syncCue) 0)
               (= prevCue (| (& prevCue $fff0) syncCue))
            else
               (= prevCue syncCue)
            )
            (DoSync NextSync self)  ;; get next syncTime and syncCue
         )
      )
   )

   (method (syncStop)
      (= prevCue -1)
      (DoSync StopSync)
   )
)



(class ScriptSync kindof Object
   (properties
      prevSignal  -1
      playing     0
   )
   (methods
      cue
      handleEvent
   )
   
   (method (init modNum noun verb case seq)
      (if theSync
         (self cue)
      )
      ((= theSync (Sync new:))
         init:       ,
         syncStart:  modNum noun verb case seq
      )
      (if (!= (theSync prevCue) -1)
         (= playing TRUE)
         (regions add: self)
      )
      (Timer setTicks: self (DoAudio WPlay modNum noun verb case seq))
   )

   (method (doit &tmp oldSignal)
      (if (!= (theSync prevCue) -1)
         (repeat
            (if (== (theSync prevCue?) -1)
               (break)
            )
            (= oldSignal (theSync syncTime?))
            (theSync syncCheck)
            (if (== oldSignal (theSync syncTime?))
               (break)
            )
         )
         (= prevSignal (theSync prevCue))
      )
   )

   (method (cue)
      (Animate (cast elements?) FALSE)
      (= playing FALSE)
      (= prevSignal 32767)
      (regions delete: self)
      (if theSync
         (theSync syncStop:, dispose:)
         (= theSync 0)
      )
   )
)



(class MouthSync kindof Cycle
   (methods
      cue
   )
   (method (init theObj modnum noun verb case seq) 
      (super init: theObj)
      (if (IsObject theSync)
         (theSync 
            syncStop: ,
            dispose:
         )
      )
      ((= theSync (Sync new:))
         syncStart: modnum noun verb case seq
      )
   )

   (method (doit &tmp newCel oldSignal theTime cntr)
      (super doit:)

      (if (!= (theSync prevCue?) -1)
         (= theTime (theSync syncTime?))
         (= cntr 0)
         (repeat
            (= oldSignal (theSync syncTime?))
            (theSync syncCheck:)
            (if (== oldSignal (theSync syncTime?))
               (break)
            )
         )
         (if (and (!= theTime (theSync syncTime?))
                  (!= (client cel?) (= newCel (& $F (theSync prevCue?))))
            )
;            (= numCels (client lastCel?))
;            (if (or  (< newCel 0)
;                     (> newCel numCels)
;               )
;               (if (<= numCels 1)
;                  (= newCel numCels)
;               else
;                  (repeat
;                     (if (!= (client cel?) (= newCel (Random 1 numCels)))
;                        (break)
;                     )
;                  )
;               )
;            )
            (client cel: newCel)
         )
      else
         (= completed TRUE)
         (self cycleDone:)
      )
   )

   (method (dispose)
      (super dispose:)
      (if theSync
         (theSync dispose:)
         (= theSync 0)
      )
   )

   (method (cue)
      (if theSync
         (theSync 
            syncStop:,
            dispose:
         )
         (= theSync 0)
         (if caller
            (caller cue:)
            (= caller 0)
         )
      )
   )
)


