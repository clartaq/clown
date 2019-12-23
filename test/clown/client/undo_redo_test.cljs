;;;-----------------------------------------------------------------------------
;;; Tests of the undo/redo functionality.

(ns clown.client.undo-redo-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clown.client.util.undo-redo :as ur]))

(deftest get-a-new-UndoManager-test
  (testing "That a new undo manager is created and initialized correctly."
    (let [atom-to-track (atom "first state")
          um (ur/undo-manager atom-to-track)]
      (is (= 1 (ur/num-undos um)))
      (is (not (ur/can-undo? um)))
      (is (zero? (ur/num-redos um))))))

(deftest save-new-state-test
  (testing "That new state can be saved in the undo manager."
    (let [first-state "first state"
          atom-to-track (atom first-state)
          second-state "the second state"
          third-state "and a third state"
          um (ur/undo-manager atom-to-track)]
      (reset! atom-to-track second-state)
      (reset! atom-to-track third-state)
      (is (= 3 (ur/num-undos um)))
      (is (true? (ur/can-undo? um)))
      (is (zero? (ur/num-redos um))))))

(deftest can-undo-state-changes
  (testing "The stored changes in state can be retrieved and undone."
    (let [first-state "first state"
          atom-to-track (atom first-state)
          second-state "the second state"
          third-state "and a third state"
          um (ur/undo-manager atom-to-track)]
      ;; Add two new states.
      (reset! atom-to-track second-state)
      (reset! atom-to-track third-state)
      ;; See if we can restore the second state.
      (let [retrieved-state (ur/undo! um)]
        (is (= second-state retrieved-state))
        ;; Make sure that the atom we are tracking has, in fact, been restored.
        (is (= second-state @atom-to-track))
        (is (true? (ur/can-undo? um)))
        (is (true? (ur/can-redo? um)))
        (is (= 2 (ur/num-undos um)))
        (is (= 1 (ur/num-redos um))))
      ;; See if we can restore the initial state.
      (let [retrieved-state (ur/undo! um)]
        (is (= first-state retrieved-state))
        ;; Make sure that the atom we are tracking has, in fact, been restored.
        (is (= first-state @atom-to-track))
        (is (false? (ur/can-undo? um)))
        (is (true? (ur/can-redo? um)))
        (is (= 1 (ur/num-undos um)))
        (is (= 2 (ur/num-redos um)))))))

(deftest can-redo-state-changes
  (testing "The stored changes in state can be retrieved, undone and redone."
    (let [first-state "first state"
          atom-to-track (atom first-state)
          second-state "the second state"
          third-state "and a third state"
          um (ur/undo-manager atom-to-track)]
      ;; Add two new states.
      (reset! atom-to-track second-state)
      (reset! atom-to-track third-state)
      ;; See if we can restore the second state.
      (let [retrieved-state (ur/undo! um)]
        (is (= second-state retrieved-state))
        ;; Make sure that the atom we are tracking has, in fact, been restored.
        (is (= second-state @atom-to-track))
        (is (true? (ur/can-undo? um)))
        (is (true? (ur/can-redo? um)))
        (is (= 2 (ur/num-undos um)))
        (is (= 1 (ur/num-redos um))))
      ;; See if we can redo! the third state.
      (let [retrieved-state (ur/redo! um)]
        (is (= third-state retrieved-state))
        ;; Make sure that the atom we are tracking has, in fact, been restored.
        (is (= third-state @atom-to-track))
        (is (true? (ur/can-undo? um)))
        (is (false? (ur/can-redo? um)))
        (is (= 3 (ur/num-undos um)))
        (is (zero? (ur/num-redos um)))))))

(deftest assure-nothing-changes-when-paused-test
  (testing "That state changes when paused are not recorded."
    (let [first-state "first state"
          atom-to-track (atom first-state)
          second-state "the second state"
          third-state "and a third state"
          um (ur/undo-manager atom-to-track)]
      (ur/pause-tracking! um)
      (reset! atom-to-track second-state)
      (ur/resume-tracking! um)
      (reset! atom-to-track third-state)
      (let [retrieved-state (ur/undo! um)]
        (is (= first-state retrieved-state))
        (is (false? (ur/can-undo? um)))
        (is (true? (ur/can-redo? um)))
        (is (= 1 (ur/num-undos um)))
        (is (= 1 (ur/num-redos um)))))))

(deftest assure-records-are-emptied-when-stopped-test
  (testing "That state changes when stopped are not recorded."
    (let [first-state "first state"
          atom-to-track (atom first-state)
          second-state "the second state"
          third-state "and a third state"
          um (ur/undo-manager atom-to-track)]
      (ur/stop-tracking! um)
      (reset! atom-to-track second-state)
      (reset! atom-to-track third-state)
      (let [retrieved-state (ur/undo! um)]
        (is (nil? retrieved-state))
        (is (false? (ur/can-undo? um)))
        (is (false? (ur/can-redo? um)))
        (is (zero? (ur/num-undos um)))
        (is (zero? (ur/num-redos um)))))))
